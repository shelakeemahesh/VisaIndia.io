package com.visasetu.controller;

import com.visasetu.domain.entity.Trip;
import com.visasetu.security.SecurityUtils;
import com.visasetu.service.PaymentService;
import com.visasetu.service.PaymentSseService;
import com.visasetu.service.TripService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSseService paymentSseService;
    private final TripService tripService;

    @Data
    public static class CreateOrderDto {
        private String referenceId;
    }

    @Data
    public static class ResyncDto {
        private String referenceId;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestBody CreateOrderDto request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Idempotency-Key header is required."));
        }
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Application Reference ID is required."));
        }

        // Authorization and ownership check
        Optional<Trip> tripOpt = tripService.getTripByReference(request.getReferenceId().trim());
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }
        Trip trip = tripOpt.get();
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (trip.getUserId() != null) {
            boolean isOwner = currentUserId.isPresent() && trip.getUserId().equals(currentUserId.get());
            if (!isOwner && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("IDOR attempt: User {} attempted to create payment order for trip {} owned by {}",
                        currentUserId.orElse(null), request.getReferenceId(), trip.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to initiate payment for this application."));
            }
        } else if (trip.getGuestAccessToken() != null) {
            boolean isMatchingToken = guestAccessToken != null
                    && java.security.MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            guestAccessToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("IDOR attempt: Missing/invalid credentials for payment order on unowned trip {}", request.getReferenceId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to initiate payment for this application."));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("IDOR attempt: User {} attempted to create payment order for guest trip {} with mismatched email",
                        currentUserId.orElse(null), request.getReferenceId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to initiate payment for this application."));
            }
        }

        try {
            Map<String, Object> order = paymentService.createPaymentOrder(request.getReferenceId(), idempotencyKey);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @jakarta.validation.Valid @RequestBody com.visasetu.dto.PaymentVerificationDto request
    ) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Application Reference ID is required."));
        }

        Optional<Trip> tripOpt = tripService.getTripByReference(request.getReferenceId().trim());
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }
        Trip trip = tripOpt.get();
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (trip.getUserId() != null) {
            boolean isOwner = currentUserId.isPresent() && trip.getUserId().equals(currentUserId.get());
            if (!isOwner && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to verify payment for this application."));
            }
        } else if (trip.getGuestAccessToken() != null) {
            boolean isMatchingToken = guestAccessToken != null
                    && java.security.MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(StandardCharsets.UTF_8),
                            guestAccessToken.trim().getBytes(StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to verify payment for this application."));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to verify payment for this application."));
            }
        }

        try {
            Map<String, Object> result = paymentService.verifyPayment(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class RefundRequestDto {
        private String refundId;
        private java.math.BigDecimal amount;
        private String reason;
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable String orderId,
            @RequestBody(required = false) RefundRequestDto request
    ) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only administrators can initiate refunds."));
        }
        try {
            String refundId = request != null ? request.getRefundId() : null;
            java.math.BigDecimal amount = request != null ? request.getAmount() : null;
            String reason = request != null ? request.getReason() : null;
            Map<String, Object> result = paymentService.processRefund(orderId, refundId, amount, reason);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestBody byte[] rawPayloadBytes
    ) {
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook rejected: Missing X-Razorpay-Signature header.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "REJECTED", "error", "Missing signature header."));
        }

        String rawPayload = new String(rawPayloadBytes, StandardCharsets.UTF_8);
        log.info("Received Razorpay webhook. Event-ID header: {}, payload length: {}", eventId, rawPayload.length());

        boolean processed = paymentService.processWebhook(rawPayload, signature.trim(), eventId);
        if (!processed) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "REJECTED"));
        }

        return ResponseEntity.ok(Map.of("status", "PROCESSED"));
    }

    @PostMapping("/resync")
    public ResponseEntity<?> resyncPayment(
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestBody ResyncDto request
    ) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID is required."));
        }

        // Ownership check
        Optional<Trip> tripOpt = tripService.getTripByReference(request.getReferenceId().trim());
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }
        Trip trip = tripOpt.get();
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (trip.getUserId() != null) {
            boolean isOwner = currentUserId.isPresent() && trip.getUserId().equals(currentUserId.get());
            if (!isOwner && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to resync payment for this application."));
            }
        } else if (trip.getGuestAccessToken() != null) {
            boolean isMatchingToken = guestAccessToken != null
                    && java.security.MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            guestAccessToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to resync payment for this application."));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to resync payment for this application."));
            }
        }

        try {
            Map<String, Object> result = paymentService.reconcilePayment(request.getReferenceId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/{referenceId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPaymentEvents(
            @PathVariable String referenceId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestParam(value = "token", required = false) String guestTokenParam
    ) {
        Optional<Trip> tripOpt = tripService.getTripByReference(referenceId.trim());
        if (tripOpt.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found.");
        }
        Trip trip = tripOpt.get();
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (trip.getUserId() != null) {
            boolean isOwner = currentUserId.isPresent() && trip.getUserId().equals(currentUserId.get());
            if (!isOwner && !SecurityUtils.hasRole("ADMIN")) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to subscribe to this application.");
            }
        } else if (trip.getGuestAccessToken() != null) {
            String token = guestAccessToken != null ? guestAccessToken : guestTokenParam;
            boolean isMatchingToken = token != null
                    && java.security.MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            token.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to subscribe to this application.");
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to subscribe to this application.");
            }
        }

        log.info("Client subscribed to SSE live status for {}", referenceId);
        return paymentSseService.subscribe(referenceId);
    }
}
