package com.visasetu.controller;

import com.visasetu.domain.entity.DemoApplication;
import com.visasetu.domain.entity.Trip;
import com.visasetu.repository.DemoApplicationRepository;
import com.visasetu.repository.TripRepository;
import com.visasetu.service.OtpService;
import com.visasetu.service.PaymentService;
import com.visasetu.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
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

    private final TripRepository tripRepository;
    private final DemoApplicationRepository demoApplicationRepository;
    private final OtpService otpService;
    private final PaymentService paymentService;
    private final RateLimitingService rateLimitingService;

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
        private String preparationStatus;
        private String officialPortalUrl;
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
    public ResponseEntity<?> trackApplication(@RequestBody TrackRequest request, HttpServletRequest httpRequest) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank() ||
            request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID and applicant email are required."));
        }

        String ref = request.getReferenceId().trim();
        String email = request.getEmail().trim().toLowerCase();

        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimitingService.tryAcquire("track:" + email + ":" + clientIp, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many tracking attempts. Please wait a minute."
            ));
        }

        // 1. Look up in real system of record first
        Optional<Trip> tripOpt = tripRepository.findByReferenceIdIgnoreCase(ref)
                .filter(t -> t.getApplicantEmail().equalsIgnoreCase(email));

        // 2. Fallback to demo application records if present
        Optional<DemoApplication> demoOpt = Optional.empty();
        if (tripOpt.isEmpty()) {
            demoOpt = demoApplicationRepository.findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase(ref, email);
        }

        if (tripOpt.isEmpty() && demoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Application reference or verification details could not be verified. Please check the details and try again."
            ));
        }

        // If OTP is not provided, send OTP prompt
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            otpService.generateOtp(email, "TRACKING");
            return ResponseEntity.ok(Map.of(
                    "status", "OTP_REQUIRED",
                    "message", "A 6-digit security code was dispatched to " + maskEmail(email) + ".",
                    "referenceId", ref
            ));
        }

        // Purpose-bound OTP verification
        boolean validOtp = otpService.verifyOtp(email, request.getOtp().trim(), "TRACKING");
        if (!validOtp) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired 6-digit verification code."
            ));
        }

        if (tripOpt.isPresent()) {
            Trip trip = tripOpt.get();
            List<TimelineStep> timeline = buildTripTimeline(trip);
            return ResponseEntity.ok(TrackResponse.builder()
                    .referenceId(trip.getReferenceId())
                    .applicantName(trip.getApplicantName())
                    .maskedEmail(maskEmail(trip.getApplicantEmail()))
                    .nationality(trip.getNationality())
                    .categoryName(trip.getCategoryName())
                    .status(trip.getStatus())
                    .preparationStatus(trip.getPreparationStatus() != null ? trip.getPreparationStatus() : "PREPARATION_IN_PROGRESS")
                    .officialPortalUrl("https://indianvisaonline.gov.in/evisa/Registration")
                    .amount(trip.getTotalAmount() != null ? trip.getTotalAmount().doubleValue() : null)
                    .arrivalDate(trip.getArrivalDate() != null ? trip.getArrivalDate().toString() : null)
                    .portOfArrival(trip.getPortOfArrival())
                    .qrData(trip.getQrToken())
                    .createdAt(trip.getCreatedAt())
                    .updatedAt(trip.getUpdatedAt())
                    .timeline(timeline)
                    .build());
        } else {
            DemoApplication app = demoOpt.get();
            List<TimelineStep> timeline = buildDemoTimeline(app);
            return ResponseEntity.ok(TrackResponse.builder()
                    .referenceId(app.getReferenceId())
                    .applicantName(app.getApplicantName())
                    .maskedEmail(maskEmail(app.getApplicantEmail()))
                    .nationality(app.getNationality())
                    .categoryName(app.getCategoryName())
                    .status(app.getStatus())
                    .preparationStatus("PREPARATION_IN_PROGRESS")
                    .officialPortalUrl("https://indianvisaonline.gov.in/evisa/Registration")
                    .orderId(app.getOrderId())
                    .paymentId(app.getPaymentId())
                    .amount(app.getAmount() != null ? app.getAmount().doubleValue() : null)
                    .arrivalDate(app.getArrivalDate() != null ? app.getArrivalDate().toString() : null)
                    .portOfArrival(app.getPortOfArrival())
                    .qrData(app.getQrData())
                    .createdAt(app.getCreatedAt())
                    .updatedAt(app.getUpdatedAt())
                    .timeline(timeline)
                    .build());
        }
    }

    @PostMapping("/resync-payment")
    public ResponseEntity<?> resyncPayment(@RequestBody ResyncPaymentRequest request) {
        if (request.getReferenceId() == null || request.getReferenceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reference ID is required."));
        }

        String ref = request.getReferenceId().trim();
        log.info("Triggering authoritative payment reconciliation for application {}", ref);

        // Call authoritative payment reconciliation service without fabricating success
        Map<String, Object> result = paymentService.reconcilePayment(ref);
        return ResponseEntity.ok(result);
    }

    private List<TimelineStep> buildTripTimeline(Trip trip) {
        String status = trip.getStatus();
        Instant base = trip.getCreatedAt();

        return List.of(
                TimelineStep.builder()
                        .step("DRAFT")
                        .label("Preparation Draft")
                        .state("COMPLETED")
                        .description("Profile, passport specs and travel itinerary collected.")
                        .timestamp(base)
                        .build(),
                TimelineStep.builder()
                        .step("SUBMITTED")
                        .label("Dossier Registered")
                        .state(isPastOrCurrent(status, "SUBMITTED"))
                        .description("Application snapshot recorded in preparation system.")
                        .timestamp(trip.getSubmittedAt() != null ? trip.getSubmittedAt() : base)
                        .build(),
                TimelineStep.builder()
                        .step("PAYMENT")
                        .label("Assistance Fee")
                        .state(getPaymentStepState(status))
                        .description("Preparation & document review service fee settlement.")
                        .timestamp(base.plusSeconds(300))
                        .build(),
                TimelineStep.builder()
                        .step("UNDER_REVIEW")
                        .label("Document Verification")
                        .state(isPastOrCurrent(status, "UNDER_REVIEW"))
                        .description("Photo specifications & passport bio-page validation.")
                        .timestamp(base.plusSeconds(600))
                        .build(),
                TimelineStep.builder()
                        .step("FINAL_DECISION")
                        .label("Official Portal Handoff")
                        .state("READY_FOR_OFFICIAL_HANDOFF".equalsIgnoreCase(trip.getPreparationStatus()) || "APPROVED".equals(status) ? "COMPLETED" : ("REJECTED".equals(status) ? "FAILED" : "PENDING"))
                        .description("READY_FOR_OFFICIAL_HANDOFF".equalsIgnoreCase(trip.getPreparationStatus()) || "APPROVED".equals(status)
                                ? "Application verified. Ready for submission on official portal: https://indianvisaonline.gov.in"
                                : ("REJECTED".equals(status) ? "Preparation flagged requirements conflict" : "Awaiting document verification"))
                        .timestamp("APPROVED".equals(status) || "REJECTED".equals(status) ? trip.getUpdatedAt() : null)
                        .build()
        );
    }

    private List<TimelineStep> buildDemoTimeline(DemoApplication app) {
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
                        .description("Payment Gateway settlement and verification.")
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
