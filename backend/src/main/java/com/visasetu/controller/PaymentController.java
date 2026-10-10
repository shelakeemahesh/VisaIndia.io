package com.visasetu.controller;

import com.visasetu.service.PaymentService;
import com.visasetu.service.PaymentSseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSseService paymentSseService;

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
            @RequestBody CreateOrderDto request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Idempotency-Key header is required."));
        }
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Application Reference ID is required."));
        }

        try {
            Map<String, Object> order = paymentService.createPaymentOrder(request.getReferenceId(), idempotencyKey);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Received Razorpay webhook. Event-ID: {}", eventId);

        String eventType = (String) payload.getOrDefault("event", "payment.captured");
        @SuppressWarnings("unchecked")
        Map<String, Object> eventPayload = (Map<String, Object>) payload.get("payload");

        String orderId = null;
        String paymentId = null;

        if (eventPayload != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentObj = (Map<String, Object>) eventPayload.get("payment");
            if (paymentObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entity = (Map<String, Object>) paymentObj.get("entity");
                if (entity != null) {
                    orderId = (String) entity.get("order_id");
                    paymentId = (String) entity.get("id");
                }
            }
        }

        if (orderId == null) {
            orderId = (String) payload.get("order_id");
        }

        String rawJson = payload.toString();
        // Fallback demo signature allowance if webhook is called in simulated test mode
        String effectiveSig = signature != null ? signature : "test_sig";

        boolean processed = paymentService.processWebhook(rawJson, effectiveSig, eventId, orderId, paymentId, eventType);
        if (!processed) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "REJECTED"));
        }

        return ResponseEntity.ok(Map.of("status", "PROCESSED"));
    }

    @PostMapping("/resync")
    public ResponseEntity<?> resyncPayment(@RequestBody ResyncDto request) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID is required."));
        }

        try {
            Map<String, Object> result = paymentService.forceReconciliation(request.getReferenceId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/{referenceId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPaymentEvents(@PathVariable String referenceId) {
        log.info("Client subscribed to SSE live status for {}", referenceId);
        return paymentSseService.subscribe(referenceId);
    }
}
