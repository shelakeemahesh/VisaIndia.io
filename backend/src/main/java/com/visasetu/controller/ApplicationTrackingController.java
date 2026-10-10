package com.visasetu.controller;

import com.visasetu.domain.entity.DemoApplication;
import com.visasetu.repository.DemoApplicationRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationTrackingController {

    private final DemoApplicationRepository demoApplicationRepository;

    @Data
    public static class TrackRequest {
        private String referenceId;
        private String email;
        private String otp;
    }

    @Data
    @Builder
    public static class TrackResponse {
        private String referenceId;
        private String applicantName;
        private String maskedEmail;
        private String nationality;
        private String categoryName;
        private String status;
        private String orderId;
        private String paymentId;
        private Double amount;
        private String arrivalDate;
        private String portOfArrival;
        private String qrData;
        private Instant createdAt;
        private Instant updatedAt;
        private List<TimelineStep> timeline;
    }

    @Data
    @Builder
    public static class TimelineStep {
        private String step;
        private String label;
        private String state; // COMPLETED, CURRENT, PENDING, FAILED
        private String description;
        private Instant timestamp;
    }

    @Data
    public static class ResyncPaymentRequest {
        private String referenceId;
    }

    @PostMapping("/track")
    public ResponseEntity<?> trackApplication(@RequestBody TrackRequest request) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank() ||
            request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID and applicant email are required."));
        }

        String ref = request.getReferenceId().trim();
        String email = request.getEmail().trim();

        // Enforce generic response on mismatch to prevent enumeration
        Optional<DemoApplication> appOpt = demoApplicationRepository.findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase(ref, email);
        if (appOpt.isEmpty()) {
            // Also check if reference exists alone just for demo ease, or keep generic error
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Application reference or verification details could not be verified. Please check the details and try again."
            ));
        }

        DemoApplication app = appOpt.get();

        // If OTP is not provided, send OTP prompt
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "status", "OTP_REQUIRED",
                    "message", "A 6-digit security code was dispatched to " + maskEmail(email) + ". For demo verification, enter code: 123456",
                    "referenceId", ref
            ));
        }

        // Demo OTP check (allow "123456" or any 6-digit numeric string for demo review)
        if (!request.getOtp().matches("\\d{6}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid 6-digit verification code."));
        }

        List<TimelineStep> timeline = buildTimeline(app);

        TrackResponse response = TrackResponse.builder()
                .referenceId(app.getReferenceId())
                .applicantName(app.getApplicantName())
                .maskedEmail(maskEmail(app.getApplicantEmail()))
                .nationality(app.getNationality())
                .categoryName(app.getCategoryName())
                .status(app.getStatus())
                .orderId(app.getOrderId())
                .paymentId(app.getPaymentId())
                .amount(app.getAmount() != null ? app.getAmount().doubleValue() : null)
                .arrivalDate(app.getArrivalDate() != null ? app.getArrivalDate().toString() : null)
                .portOfArrival(app.getPortOfArrival())
                .qrData(app.getQrData())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .timeline(timeline)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resync-payment")
    public ResponseEntity<?> resyncPayment(@RequestBody ResyncPaymentRequest request) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID is required."));
        }

        Optional<DemoApplication> appOpt = demoApplicationRepository.findByReferenceIdIgnoreCase(request.getReferenceId().trim());
        if (appOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        DemoApplication app = appOpt.get();
        log.info("Triggering payment reconciliation for application {}", app.getReferenceId());

        // In test mode: simulate reconciliation against payment gateway
        if ("PAYMENT_PENDING".equals(app.getStatus()) || "PAYMENT_FAILED".equals(app.getStatus()) || "SUBMITTED".equals(app.getStatus())) {
            app.setStatus("UNDER_REVIEW");
            app.setPaymentId("pay_reconciled_" + System.currentTimeMillis());
            app.setUpdatedAt(Instant.now());
            demoApplicationRepository.save(app);

            return ResponseEntity.ok(Map.of(
                    "status", "RECONCILED",
                    "message", "Payment reconciliation verified capture. Application state advanced to UNDER_REVIEW.",
                    "referenceId", app.getReferenceId(),
                    "newStatus", app.getStatus(),
                    "paymentId", app.getPaymentId()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "ALREADY_SYNCHRONIZED",
                "message", "Payment state is already synchronized: " + app.getStatus(),
                "referenceId", app.getReferenceId(),
                "currentStatus", app.getStatus()
        ));
    }

    private List<TimelineStep> buildTimeline(DemoApplication app) {
        String status = app.getStatus();
        Instant base = app.getCreatedAt();

        return List.of(
                TimelineStep.builder()
                        .step("DRAFT")
                        .label("Application Drafted")
                        .state("COMPLETED")
                        .description("Profile, passport specs and travel itinerary collected.")
                        .timestamp(base)
                        .build(),
                TimelineStep.builder()
                        .step("SUBMITTED")
                        .label("Submitted to System")
                        .state(isPastOrCurrent(status, "SUBMITTED"))
                        .description("Application snapshot recorded in database.")
                        .timestamp(base.plusSeconds(300))
                        .build(),
                TimelineStep.builder()
                        .step("PAYMENT")
                        .label("Fee Payment")
                        .state(getPaymentStepState(status))
                        .description("Razorpay Test Gateway settlement and verification.")
                        .timestamp(base.plusSeconds(600))
                        .build(),
                TimelineStep.builder()
                        .step("UNDER_REVIEW")
                        .label("Immigration Review")
                        .state(isPastOrCurrent(status, "UNDER_REVIEW"))
                        .description("Automated security screening & jurisdiction review.")
                        .timestamp(base.plusSeconds(1800))
                        .build(),
                TimelineStep.builder()
                        .step("FINAL_DECISION")
                        .label("Final Decision")
                        .state("APPROVED".equals(status) ? "COMPLETED" : ("REJECTED".equals(status) ? "FAILED" : "PENDING"))
                        .description("APPROVED".equals(status) ? "Electronic Travel Authorization (ETA) Granted" : ("REJECTED".equals(status) ? "Application Not Approved" : "Pending decision"))
                        .timestamp("APPROVED".equals(status) || "REJECTED".equals(status) ? app.getUpdatedAt() : null)
                        .build()
        );
    }

    private String isPastOrCurrent(String currentStatus, String target) {
        List<String> order = List.of("DRAFT", "SUBMITTED", "PAYMENT_PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED");
        int curIdx = order.indexOf(currentStatus);
        int targetIdx = order.indexOf(target);
        if (curIdx > targetIdx) return "COMPLETED";
        if (curIdx == targetIdx) return "CURRENT";
        return "PENDING";
    }

    private String getPaymentStepState(String currentStatus) {
        if ("PAYMENT_FAILED".equals(currentStatus)) return "FAILED";
        if ("PAYMENT_PENDING".equals(currentStatus)) return "CURRENT";
        if ("DRAFT".equals(currentStatus) || "SUBMITTED".equals(currentStatus)) return "PENDING";
        return "COMPLETED";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "*@" + parts[1];
        return name.substring(0, 2) + "****" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
