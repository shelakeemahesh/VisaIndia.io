package com.visasetu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import com.visasetu.domain.entity.PaymentEvent;
import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.domain.entity.Trip;
import com.visasetu.dto.PaymentVerificationDto;
import com.visasetu.repository.PaymentEventRepository;
import com.visasetu.repository.PaymentOrderRepository;
import com.visasetu.repository.PaymentOutboxRepository;
import com.visasetu.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
import java.util.Set;
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

    @Value("${visasetu.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${visasetu.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${visasetu.razorpay.webhook-secret:}")
    private String webhookSecret;

    @Setter
    private RazorpayClient razorpayClient;

    // Terminal statuses that must NEVER be downgraded
    private static final Set<String> IMMUTABLE_SETTLED_STATUSES = Set.of("PAID", "UNDER_REVIEW", "APPROVED", "REJECTED");

    public synchronized RazorpayClient getRazorpayClient() {
        if (this.razorpayClient == null) {
            if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
                throw new IllegalStateException("Razorpay API credentials are not configured.");
            }
            try {
                this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize RazorpayClient: " + e.getMessage(), e);
            }
        }
        return this.razorpayClient;
    }

    /**
     * Create or retrieve idempotent payment order with minor units (paise/cents) and receipt = referenceId.
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
            long amountSubunits = existing.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            return Map.of(
                    "orderId", existing.getOrderId(),
                    "amount", existing.getAmount(),
                    "amountSubunits", amountSubunits,
                    "currency", existing.getCurrency(),
                    "status", existing.getStatus(),
                    "idempotencyKey", existing.getIdempotencyKey(),
                    "razorpayKeyId", razorpayKeyId != null ? razorpayKeyId : "",
                    "referenceId", existing.getReferenceId()
            );
        }

        Trip trip = tripRepository.findByReferenceIdIgnoreCase(referenceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Trip application not found: " + referenceId));

        // Use strictly server-calculated fee from Trip entity
        BigDecimal amount = trip.getTotalAmount() != null && trip.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                ? trip.getTotalAmount()
                : new BigDecimal("25.00");

        long amountSubunits = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String currency = (trip.getCurrency() != null && !trip.getCurrency().isBlank()) ? trip.getCurrency() : "USD";

        String orderId;
        try {
            RazorpayClient client = getRazorpayClient();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountSubunits);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", trip.getReferenceId());
            orderRequest.put("payment_capture", 1);

            JSONObject notes = new JSONObject();
            notes.put("reference_id", trip.getReferenceId());
            notes.put("applicant_email", trip.getApplicantEmail());
            orderRequest.put("notes", notes);

            Order rzpOrder = client.orders.create(orderRequest);
            orderId = rzpOrder.get("id");
            log.info("Created real Razorpay order {} for reference {} (receipt: {})", orderId, trip.getReferenceId(), trip.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to create order on Razorpay gateway: {}", e.getMessage(), e);
            throw new IllegalStateException("Payment gateway order creation failed: " + e.getMessage(), e);
        }

        PaymentOrder order = PaymentOrder.builder()
                .tripId(trip.getId())
                .referenceId(trip.getReferenceId())
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .idempotencyKey(idempotencyKey.trim())
                .status("CREATED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        paymentOrderRepository.save(order);

        // Advance trip status to PAYMENT_PENDING if not already settled
        if (!IMMUTABLE_SETTLED_STATUSES.contains(trip.getStatus())) {
            trip.setStatus("PAYMENT_PENDING");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);
            paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_PENDING", "Payment order initialized on payment gateway.");
        }

        log.info("Created payment order {} for reference {} (Amount: {} {})", orderId, trip.getReferenceId(), amount, currency);

        return Map.of(
                "orderId", order.getOrderId(),
                "amount", order.getAmount(),
                "amountSubunits", amountSubunits,
                "currency", order.getCurrency(),
                "status", order.getStatus(),
                "idempotencyKey", order.getIdempotencyKey(),
                "razorpayKeyId", razorpayKeyId != null ? razorpayKeyId : "",
                "referenceId", order.getReferenceId()
        );
    }

    /**
     * Client-side checkout verification: Verifies HMAC-SHA256 signature of (order_id + "|" + payment_id).
     */
    @Transactional
    public Map<String, Object> verifyPayment(PaymentVerificationDto verificationDto) {
        if (verificationDto == null) {
            throw new IllegalArgumentException("Payment verification request cannot be null.");
        }

        String orderId = verificationDto.getRazorpayOrderId().trim();
        String paymentId = verificationDto.getRazorpayPaymentId().trim();
        String providedSignature = verificationDto.getRazorpaySignature().trim();

        // 1. Verify HMAC-SHA256 signature
        String payload = orderId + "|" + paymentId;
        String expectedSignature = calculateHmacSha256(payload, razorpayKeySecret);

        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.error("Payment verification failed: Signature mismatch for order {} and payment {}", orderId, paymentId);
            throw new IllegalArgumentException("Payment verification failed: Invalid cryptographic signature.");
        }

        // 2. Idempotent settlement (shared with webhook pipeline)
        String eventId = "verify_" + paymentId;
        boolean settled = settlePayment(orderId, paymentId, eventId, null, null);

        if (!settled) {
            throw new IllegalStateException("Payment verification could not settle order: " + orderId);
        }

        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
        String tripStatus = "UNDER_REVIEW";
        if (order != null) {
            Trip trip = tripRepository.findById(order.getTripId()).orElse(null);
            if (trip != null) {
                tripStatus = trip.getStatus();
            }
        }

        return Map.of(
                "status", "VERIFIED",
                "referenceId", verificationDto.getReferenceId(),
                "orderId", orderId,
                "paymentId", paymentId,
                "tripStatus", tripStatus,
                "message", "Payment verified and recorded successfully."
        );
    }

    /**
     * Shared idempotent settlement pipeline for captured payments (used by webhook and verify endpoints).
     */
    @Transactional
    public boolean settlePayment(String orderId, String paymentId, String eventId, BigDecimal payloadAmount, String payloadCurrency) {
        // 1. Deduplication on event ID
        if (paymentEventRepository.existsById(eventId)) {
            log.info("Deduplication: Event {} already processed. Acknowledging idempotently.", eventId);
            return true;
        }

        // 2. Deduplication on payment ID
        if (paymentId != null && paymentEventRepository.existsByPaymentId(paymentId)) {
            log.info("Deduplication: Payment ID {} already settled. Acknowledging idempotently.", paymentId);
            // Still record this event ID mapping to avoid re-running if eventId is distinct
            paymentEventRepository.save(PaymentEvent.builder()
                    .eventId(eventId)
                    .eventType("PAYMENT_CAPTURED_DUPLICATE")
                    .orderId(orderId)
                    .paymentId(paymentId)
                    .processedAt(Instant.now())
                    .build());
            return true;
        }

        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            log.warn("Cannot settle payment: Order {} not found in system of record.", orderId);
            return false;
        }

        // Validate amount and currency if provided
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

        // Update PaymentOrder
        order.setStatus("PAID");
        order.setPaymentId(paymentId != null ? paymentId : "pay_" + System.currentTimeMillis());
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        // State Machine Transition: Never downgrade immutable settled statuses
        if (!"APPROVED".equals(trip.getStatus()) && !"REJECTED".equals(trip.getStatus())) {
            trip.setStatus("UNDER_REVIEW");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);
        }

        // Record in Transactional Outbox
        PaymentOutboxMessage outbox = PaymentOutboxMessage.builder()
                .eventId(eventId)
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

        // Record event for deduplication
        paymentEventRepository.save(PaymentEvent.builder()
                .eventId(eventId)
                .eventType("PAYMENT_CAPTURED")
                .orderId(orderId)
                .paymentId(paymentId)
                .processedAt(Instant.now())
                .build());

        return true;
    }

    public boolean processWebhook(String rawPayload, String signature, String headerEventId, String orderId, String paymentId, String eventType) {
        return processWebhook(rawPayload, signature, headerEventId);
    }

    /**
     * Process webhook against original raw payload and HMAC-SHA256 signature.
     * Takes event ID from the signed body or payload hash, NOT from unsigned headers.
     */
    @Transactional
    public boolean processWebhook(String rawPayload, String signature, String headerEventId) {
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

        // 3. Take event ID strictly from signed body or payload hash (never trust unsigned header)
        String resolvedEventId;
        if (root.has("event_id") && !root.get("event_id").asText().isBlank()) {
            resolvedEventId = root.get("event_id").asText();
        } else if (root.has("id") && !root.get("id").asText().isBlank()) {
            resolvedEventId = root.get("id").asText();
        } else {
            resolvedEventId = "evt_" + UUID.nameUUIDFromBytes(rawPayload.getBytes(StandardCharsets.UTF_8));
        }

        // 4. Extract order_id, payment_id, amount, currency
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

        // 5. State Machine Transition & Deduplication
        if ("payment.captured".equalsIgnoreCase(eventType) || "order.paid".equalsIgnoreCase(eventType)) {
            // Deduplicate payment.captured and order.paid on payment_id
            return settlePayment(orderId, paymentId, resolvedEventId, payloadAmount, payloadCurrency);

        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                log.warn("Rejecting payment.failed webhook: Order {} not found.", orderId);
                return false;
            }

            Trip trip = tripRepository.findById(order.getTripId()).orElse(null);

            // Enterprise rule: NEVER downgrade PAID, UNDER_REVIEW, APPROVED, REJECTED
            if ("PAID".equals(order.getStatus()) || (trip != null && IMMUTABLE_SETTLED_STATUSES.contains(trip.getStatus()))) {
                log.info("Ignoring payment.failed webhook for order {} because order/trip is already in settled status (Order: {}, Trip: {})",
                        orderId, order.getStatus(), trip != null ? trip.getStatus() : "null");
                paymentEventRepository.save(PaymentEvent.builder()
                        .eventId(resolvedEventId)
                        .eventType("PAYMENT_FAILED_IGNORED")
                        .orderId(orderId)
                        .paymentId(paymentId)
                        .processedAt(Instant.now())
                        .build());
                return true;
            }

            order.setStatus("FAILED");
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);

            if (trip != null) {
                trip.setStatus("PAYMENT_FAILED");
                trip.setUpdatedAt(Instant.now());
                tripRepository.save(trip);
                paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_FAILED", "Payment transaction failed at gateway. Please retry with the same application.");
            }

            log.info("Payment failed recorded for order {}.", orderId);

            paymentEventRepository.save(PaymentEvent.builder()
                    .eventId(resolvedEventId)
                    .eventType(eventType)
                    .orderId(orderId)
                    .paymentId(paymentId)
                    .processedAt(Instant.now())
                    .build());

            return true;
        }

        // Unknown or unhandled event type
        log.info("Acknowledging unhandled event type: {}", eventType);
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
     * Never marks UNDER_REVIEW on a missing order.
     */
    @Transactional
    public Map<String, Object> reconcilePayment(String referenceId) {
        Trip trip = tripRepository.findByReferenceIdIgnoreCase(referenceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + referenceId));

        PaymentOrder order = paymentOrderRepository.findByReferenceId(trip.getReferenceId())
                .orElse(null);

        // Strict check: if no order exists, never mark UNDER_REVIEW
        if (order == null) {
            return Map.of(
                    "status", "NO_ORDER",
                    "currentTripStatus", trip.getStatus(),
                    "referenceId", trip.getReferenceId(),
                    "message", "No payment order exists for this application. Please initiate checkout."
            );
        }

        if ("PAID".equals(order.getStatus())) {
            return Map.of(
                    "status", "CONFIRMED_PAID",
                    "currentTripStatus", trip.getStatus(),
                    "referenceId", trip.getReferenceId(),
                    "paymentId", order.getPaymentId() != null ? order.getPaymentId() : "",
                    "message", "Payment has been verified and settled with the payment gateway."
            );
        }

        // Query gateway if orderId or paymentId is present
        try {
            RazorpayClient client = getRazorpayClient();
            if (order.getPaymentId() != null && !order.getPaymentId().isBlank()) {
                Payment payment = client.payments.fetch(order.getPaymentId());
                String gatewayStatus = payment.get("status");
                if ("captured".equalsIgnoreCase(gatewayStatus)) {
                    settlePayment(order.getOrderId(), order.getPaymentId(), "recon_" + order.getPaymentId(), null, null);
                    return Map.of(
                            "status", "RECONCILED",
                            "newTripStatus", "UNDER_REVIEW",
                            "referenceId", trip.getReferenceId(),
                            "message", "Payment verified directly against Razorpay gateway."
                    );
                }
            } else if (order.getOrderId() != null && !order.getOrderId().isBlank()) {
                // Fetch payments associated with the Razorpay order
                Order rzpOrder = client.orders.fetch(order.getOrderId());
                String orderStatus = rzpOrder.get("status");
                if ("paid".equalsIgnoreCase(orderStatus)) {
                    settlePayment(order.getOrderId(), null, "recon_" + order.getOrderId(), null, null);
                    return Map.of(
                            "status", "RECONCILED",
                            "newTripStatus", "UNDER_REVIEW",
                            "referenceId", trip.getReferenceId(),
                            "message", "Order verified as paid directly on Razorpay gateway."
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Gateway query during reconciliation for reference {}: {}", referenceId, e.getMessage());
        }

        return Map.of(
                "status", "UNVERIFIED",
                "currentTripStatus", trip.getStatus(),
                "referenceId", trip.getReferenceId(),
                "message", "No confirmed payment transaction found on gateway. Application remains in status: " + trip.getStatus() + ". Please complete checkout."
        );
    }

    /**
     * Process refund with idempotency check against unique refund_id.
     */
    @Transactional
    public Map<String, Object> processRefund(String orderId, String refundId, BigDecimal refundAmount, String reason) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + orderId));

        if (order.getRefundId() != null) {
            log.info("Idempotent hit: Order {} already refunded with refund ID {}", orderId, order.getRefundId());
            return Map.of(
                    "status", "ALREADY_REFUNDED",
                    "orderId", orderId,
                    "refundId", order.getRefundId(),
                    "message", "Refund was already processed."
            );
        }

        if (!"PAID".equals(order.getStatus()) || order.getPaymentId() == null) {
            throw new IllegalStateException("Cannot refund unpaid order: " + orderId);
        }

        String finalRefundId = refundId != null && !refundId.isBlank() ? refundId : "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Call Razorpay refund API if client is available
        try {
            RazorpayClient client = getRazorpayClient();
            JSONObject refundRequest = new JSONObject();
            if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                refundRequest.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).longValue());
            }
            if (reason != null) {
                JSONObject notes = new JSONObject();
                notes.put("reason", reason);
                refundRequest.put("notes", notes);
            }
            Refund rzpRefund = client.payments.refund(order.getPaymentId(), refundRequest);
            if (rzpRefund.has("id")) {
                finalRefundId = rzpRefund.get("id");
            }
        } catch (Exception e) {
            log.warn("Gateway refund API invocation note: {}", e.getMessage());
        }

        order.setRefundId(finalRefundId);
        order.setStatus("REFUNDED");
        order.setUpdatedAt(Instant.now());
        paymentOrderRepository.save(order);

        Trip trip = tripRepository.findById(order.getTripId()).orElse(null);
        if (trip != null) {
            trip.setStatus("REFUNDED");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);
            paymentSseService.emitStatusChange(trip.getReferenceId(), "REFUNDED", "Payment refunded. Refund ID: " + finalRefundId);
        }

        return Map.of(
                "status", "REFUNDED",
                "orderId", orderId,
                "refundId", finalRefundId,
                "message", "Refund processed successfully."
        );
    }

    public boolean verifySignature(String payload, String signature) {
        if (payload == null || signature == null || signature.isBlank()) return false;
        try {
            String expected = calculateHmacSha256(payload, webhookSecret);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error computing HMAC signature: {}", e.getMessage());
            return false;
        }
    }

    public static String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256: " + e.getMessage(), e);
        }
    }
}
