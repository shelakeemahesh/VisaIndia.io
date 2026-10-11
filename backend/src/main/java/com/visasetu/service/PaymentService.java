package com.visasetu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import com.visasetu.domain.entity.PaymentEvent;
import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.domain.entity.Trip;
import com.visasetu.repository.PaymentEventRepository;
import com.visasetu.repository.PaymentOrderRepository;
import com.visasetu.repository.PaymentOutboxRepository;
import com.visasetu.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final TripRepository tripRepository;
    private final PaymentSseService paymentSseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${visasetu.razorpay.key-id:rzp_test_sample_key_12345}")
    private String razorpayKeyId;

    @Value("${visasetu.razorpay.key-secret:sample_razorpay_secret_67890}")
    private String razorpayKeySecret;

    @Value("${visasetu.razorpay.webhook-secret:sample_webhook_secret_abcde}")
    private String webhookSecret;

    /**
     * Create or retrieve idempotent payment order using server-calculated fee.
     */
    @Transactional
    public Map<String, Object> createPaymentOrder(String referenceId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is mandatory for payment order initialization.");
        }

        // Idempotency check: return cached order if exists
        Optional<PaymentOrder> existingOpt = paymentOrderRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existingOpt.isPresent()) {
            PaymentOrder existing = existingOpt.get();
            log.info("Idempotent hit for key {}: Returning existing order {}", idempotencyKey, existing.getOrderId());
            return Map.of(
                    "orderId", existing.getOrderId(),
                    "amount", existing.getAmount(),
                    "currency", existing.getCurrency(),
                    "status", existing.getStatus(),
                    "idempotencyKey", existing.getIdempotencyKey(),
                    "razorpayKeyId", razorpayKeyId,
                    "referenceId", existing.getReferenceId()
            );
        }

        Trip trip = tripRepository.findByReferenceIdIgnoreCase(referenceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Trip application not found: " + referenceId));

        // Use strictly server-calculated fee from Trip entity
        BigDecimal amount = trip.getTotalAmount() != null && trip.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                ? trip.getTotalAmount()
                : new BigDecimal("25.00");

        String orderId;
        if (!razorpayKeyId.startsWith("rzp_test_sample")) {
            try {
                RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                JSONObject orderRequest = new JSONObject();
                int amountSubunits = amount.multiply(BigDecimal.valueOf(100)).intValue();
                orderRequest.put("amount", amountSubunits);
                orderRequest.put("currency", trip.getCurrency() != null ? trip.getCurrency() : "USD");
                orderRequest.put("receipt", trip.getReferenceId());
                Order rzpOrder = razorpayClient.orders.create(orderRequest);
                orderId = rzpOrder.get("id");
                log.info("Initialized live Razorpay gateway order {} for reference {}", orderId, trip.getReferenceId());
            } catch (Exception e) {
                log.error("Failed to initialize live order on Razorpay gateway: {}", e.getMessage(), e);
                throw new IllegalStateException("Payment gateway communication failed: " + e.getMessage());
            }
        } else {
            orderId = "order_rzp_" + trip.getReferenceId().replace("-", "_") + "_" + System.currentTimeMillis();
        }

        PaymentOrder order = PaymentOrder.builder()
                .tripId(trip.getId())
                .referenceId(trip.getReferenceId())
                .orderId(orderId)
                .amount(amount)
                .currency(trip.getCurrency())
                .idempotencyKey(idempotencyKey.trim())
                .status("CREATED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        paymentOrderRepository.save(order);

        // Advance trip status to PAYMENT_PENDING
        if (!"PAID".equals(trip.getStatus()) && !"UNDER_REVIEW".equals(trip.getStatus()) && !"APPROVED".equals(trip.getStatus())) {
            trip.setStatus("PAYMENT_PENDING");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);
            paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_PENDING", "Payment order initialized on payment gateway.");
        }

        log.info("Created payment order {} for reference {} (Amount: {} {})", orderId, trip.getReferenceId(), amount, trip.getCurrency());

        return Map.of(
                "orderId", order.getOrderId(),
                "amount", order.getAmount(),
                "currency", order.getCurrency(),
                "status", order.getStatus(),
                "idempotencyKey", order.getIdempotencyKey(),
                "razorpayKeyId", razorpayKeyId,
                "referenceId", order.getReferenceId()
        );
    }

    public boolean processWebhook(String rawPayload, String signature, String eventId, String orderId, String paymentId, String eventType) {
        return processWebhook(rawPayload, signature, eventId);
    }

    /**
     * Process webhook against original raw payload and HMAC-SHA256 signature.
     */
    @Transactional
    public boolean processWebhook(String rawPayload, String signature, String eventId) {
        if (rawPayload == null || signature == null || signature.isBlank()) {
            log.warn("Rejecting webhook: Missing raw body or signature.");
            return false;
        }

        // 1. Strict HMAC-SHA256 signature verification
        if (!verifySignature(rawPayload, signature)) {
            log.error("Rejecting Razorpay webhook: Signature verification failed!");
            return false;
        }

        // 2. Parse event payload
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            log.error("Rejecting webhook: Malformed JSON payload: {}", e.getMessage());
            return false;
        }

        JsonNode eventNode = root.get("event");
        if (eventNode == null || eventNode.asText().isBlank()) {
            log.warn("Rejecting webhook: Missing event type in payload.");
            return false;
        }
        String eventType = eventNode.asText();

        String resolvedEventId = eventId;
        if (resolvedEventId == null || resolvedEventId.isBlank()) {
            if (root.has("event_id")) {
                resolvedEventId = root.get("event_id").asText();
            } else if (root.has("id")) {
                resolvedEventId = root.get("id").asText();
            } else {
                resolvedEventId = "evt_" + UUID.nameUUIDFromBytes(rawPayload.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 3. Durable Deduplication via PostgreSQL payment_events table
        if (paymentEventRepository.existsById(resolvedEventId)) {
            log.warn("Durable deduplication hit: Webhook event {} already processed. Acknowledging safely without duplicate effects.", resolvedEventId);
            return true;
        }

        // Extract order_id, payment_id, amount, currency
        String orderId = null;
        String paymentId = null;
        BigDecimal payloadAmount = null;
        String payloadCurrency = null;

        JsonNode payloadObj = root.get("payload");
        if (payloadObj != null && payloadObj.has("payment")) {
            JsonNode paymentEntity = payloadObj.get("payment").get("entity");
            if (paymentEntity != null) {
                if (paymentEntity.has("order_id")) orderId = paymentEntity.get("order_id").asText();
                if (paymentEntity.has("id")) paymentId = paymentEntity.get("id").asText();
                if (paymentEntity.has("amount")) {
                    payloadAmount = new BigDecimal(paymentEntity.get("amount").asLong()).divide(new BigDecimal("100"));
                }
                if (paymentEntity.has("currency")) payloadCurrency = paymentEntity.get("currency").asText();
            }
        }

        if (orderId == null && root.has("order_id")) {
            orderId = root.get("order_id").asText();
        }

        if (orderId == null || orderId.isBlank()) {
            log.warn("Rejecting webhook: No order_id found in event {}", resolvedEventId);
            return false;
        }

        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Rejecting webhook: Order {} not found in system of record.", orderId);
            return false;
        }

        // Validate amount and currency if present
        if (payloadAmount != null && order.getAmount().compareTo(payloadAmount) != 0) {
            log.error("Amount mismatch for order {}: Expected {}, Got {}", orderId, order.getAmount(), payloadAmount);
            return false;
        }
        if (payloadCurrency != null && !order.getCurrency().equalsIgnoreCase(payloadCurrency)) {
            log.error("Currency mismatch for order {}: Expected {}, Got {}", orderId, order.getCurrency(), payloadCurrency);
            return false;
        }

        Trip trip = tripRepository.findById(order.getTripId()).orElse(null);
        if (trip == null) {
            log.warn("Trip {} not found for payment order {}", order.getTripId(), orderId);
            return false;
        }

        // State Machine Transition
        if ("payment.captured".equalsIgnoreCase(eventType) || "order.paid".equalsIgnoreCase(eventType)) {
            order.setStatus("PAID");
            order.setPaymentId(paymentId != null ? paymentId : "pay_" + System.currentTimeMillis());
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);

            trip.setStatus("UNDER_REVIEW");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            // Record in Outbox table
            PaymentOutboxMessage outbox = PaymentOutboxMessage.builder()
                    .eventId(resolvedEventId)
                    .aggregateType("TRIP_PAYMENT")
                    .aggregateId(trip.getReferenceId())
                    .eventType("PAYMENT_CAPTURED")
                    .payload("{\"referenceId\":\"" + trip.getReferenceId() + "\",\"orderId\":\"" + order.getOrderId() + "\",\"paymentId\":\"" + order.getPaymentId() + "\",\"amount\":" + order.getAmount() + "}")
                    .status("PENDING")
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();
            paymentOutboxRepository.save(outbox);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "UNDER_REVIEW", "Payment captured and confirmed by gateway. Application transitioned to consular review.");
            log.info("Payment captured for application {}. Advanced to UNDER_REVIEW.", trip.getReferenceId());

        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            order.setStatus("FAILED");
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);

            trip.setStatus("PAYMENT_FAILED");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_FAILED", "Payment transaction failed at gateway. Please retry with the same application.");
            log.info("Payment failed for application {}. Marked as PAYMENT_FAILED.", trip.getReferenceId());
        }

        // Persist deduplication event record
        paymentEventRepository.save(PaymentEvent.builder()
                .eventId(resolvedEventId)
                .eventType(eventType)
                .orderId(orderId)
                .paymentId(paymentId)
                .processedAt(Instant.now())
                .build());

        return true;
    }

    public Map<String, Object> forceReconciliation(String referenceId) {
        return reconcilePayment(referenceId);
    }

    /**
     * Authoritative payment reconciliation: Queries gateway without fabricating success.
     */
    @Transactional
    public Map<String, Object> reconcilePayment(String referenceId) {
        Trip trip = tripRepository.findByReferenceIdIgnoreCase(referenceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + referenceId));

        PaymentOrder order = paymentOrderRepository.findByReferenceId(trip.getReferenceId())
                .orElse(null);

        if (order != null && "PAID".equals(order.getStatus())) {
            return Map.of(
                    "status", "CONFIRMED_PAID",
                    "currentTripStatus", trip.getStatus(),
                    "referenceId", trip.getReferenceId(),
                    "paymentId", order.getPaymentId() != null ? order.getPaymentId() : "",
                    "message", "Payment has been verified and settled with the payment gateway."
            );
        }

        // If paymentId exists on order, query Razorpay API
        if (order != null && order.getPaymentId() != null && !order.getPaymentId().isBlank()
                && !razorpayKeyId.startsWith("rzp_test_sample")) {
            try {
                RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                Payment payment = razorpayClient.payments.fetch(order.getPaymentId());
                String gatewayStatus = payment.get("status");

                if ("captured".equalsIgnoreCase(gatewayStatus)) {
                    order.setStatus("PAID");
                    order.setUpdatedAt(Instant.now());
                    paymentOrderRepository.save(order);

                    trip.setStatus("UNDER_REVIEW");
                    trip.setUpdatedAt(Instant.now());
                    tripRepository.save(trip);

                    paymentSseService.emitStatusChange(trip.getReferenceId(), "UNDER_REVIEW", "Payment verified via direct gateway reconciliation.");

                    return Map.of(
                            "status", "RECONCILED",
                            "newTripStatus", "UNDER_REVIEW",
                            "referenceId", trip.getReferenceId(),
                            "message", "Payment verified directly against Razorpay gateway."
                    );
                }
            } catch (Exception e) {
                log.warn("Gateway query failed during reconciliation for reference {}: {}", referenceId, e.getMessage());
            }
        }

        // Strict enterprise rule: Never fabricate success when payment evidence is missing
        return Map.of(
                "status", "UNVERIFIED",
                "currentTripStatus", trip.getStatus(),
                "referenceId", trip.getReferenceId(),
                "message", "No confirmed payment transaction found on gateway. Application remains in status: " + trip.getStatus() + ". Please complete checkout."
        );
    }

    public boolean verifySignature(String payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error computing HMAC signature: {}", e.getMessage());
            return false;
        }
    }
}
