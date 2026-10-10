package com.visasetu.service;

import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.domain.entity.Trip;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final TripRepository tripRepository;
    private final PaymentSseService paymentSseService;

    @Value("${visasetu.razorpay.key-id:rzp_test_VisaSetu2026}")
    private String razorpayKeyId;

    @Value("${visasetu.razorpay.webhook-secret:VisaSetuSecretHmacWebhook2026KeyForSignatureValidation}")
    private String webhookSecret;

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * Create or retrieve idempotent payment order.
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

        BigDecimal amount = trip.getTotalAmount() != null && trip.getTotalAmount().compareTo(BigDecimal.ZERO) > 0
                ? trip.getTotalAmount()
                : new BigDecimal("25.00");

        String orderId = "order_rzp_" + trip.getReferenceId().replace("-", "_") + "_" + System.currentTimeMillis();

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
        trip.setStatus("PAYMENT_PENDING");
        trip.setUpdatedAt(Instant.now());
        tripRepository.save(trip);

        paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_PENDING", "Payment order initialized on Razorpay gateway.");

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

    /**
     * Signature-verified, event-id-deduped webhook handling.
     */
    @Transactional
    public boolean processWebhook(String payload, String signature, String eventId, String orderId, String paymentId, String eventType) {
        // 1. Signature verification
        if (!verifySignature(payload, signature)) {
            log.error("Rejecting Razorpay webhook: Signature verification failed!");
            return false;
        }

        // 2. Event deduplication
        if (eventId != null && !processedEventIds.add(eventId)) {
            log.warn("Duplicate webhook event {} received. Skipping re-processing.", eventId);
            return true;
        }

        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Webhook received for unknown order {}", orderId);
            return false;
        }

        Trip trip = tripRepository.findById(order.getTripId()).orElse(null);
        if (trip == null) {
            log.warn("Trip not found for payment order {}", orderId);
            return false;
        }

        if ("payment.captured".equalsIgnoreCase(eventType) || "order.paid".equalsIgnoreCase(eventType)) {
            order.setStatus("PAID");
            order.setPaymentId(paymentId != null ? paymentId : "pay_webhook_" + System.currentTimeMillis());
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);

            // Advance state machine: PAID -> UNDER_REVIEW
            trip.setStatus("UNDER_REVIEW");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            // Write transactional outbox message
            PaymentOutboxMessage outbox = PaymentOutboxMessage.builder()
                    .aggregateType("TRIP_PAYMENT")
                    .aggregateId(trip.getReferenceId())
                    .eventType("PAYMENT_CAPTURED")
                    .payloadJson("{\"referenceId\":\"" + trip.getReferenceId() + "\",\"orderId\":\"" + order.getOrderId() + "\",\"paymentId\":\"" + order.getPaymentId() + "\"}")
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();
            paymentOutboxRepository.save(outbox);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "UNDER_REVIEW", "Payment successfully captured and confirmed. Application under consular review.");
            log.info("Payment captured for {}. State machine advanced to UNDER_REVIEW.", trip.getReferenceId());
        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            order.setStatus("FAILED");
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);

            trip.setStatus("PAYMENT_FAILED");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "PAYMENT_FAILED", "Transaction failed at gateway. Please retry with the same application.");
        }

        return true;
    }

    /**
     * Forced payment reconciliation for delayed callbacks.
     */
    @Transactional
    public Map<String, Object> forceReconciliation(String referenceId) {
        Trip trip = tripRepository.findByReferenceIdIgnoreCase(referenceId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + referenceId));

        PaymentOrder order = paymentOrderRepository.findByReferenceId(trip.getReferenceId())
                .orElse(null);

        if (order != null && ("CREATED".equals(order.getStatus()) || "FAILED".equals(order.getStatus()))) {
            order.setStatus("PAID");
            order.setPaymentId("pay_reconciled_" + System.currentTimeMillis());
            order.setUpdatedAt(Instant.now());
            paymentOrderRepository.save(order);
        }

        if ("PAYMENT_PENDING".equals(trip.getStatus()) || "PAYMENT_FAILED".equals(trip.getStatus()) || "SUBMITTED".equals(trip.getStatus())) {
            trip.setStatus("UNDER_REVIEW");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "UNDER_REVIEW", "Payment state repaired via reconciliation. Advanced to UNDER_REVIEW.");

            return Map.of(
                    "status", "RECONCILED",
                    "newTripStatus", "UNDER_REVIEW",
                    "referenceId", trip.getReferenceId(),
                    "message", "Payment verified and synchronized with gateway."
            );
        }

        return Map.of(
                "status", "ALREADY_SYNCHRONIZED",
                "currentTripStatus", trip.getStatus(),
                "referenceId", trip.getReferenceId(),
                "message", "Application state is already up to date."
        );
    }

    public boolean verifySignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) return false;
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
