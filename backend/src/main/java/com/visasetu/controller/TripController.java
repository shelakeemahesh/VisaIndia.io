package com.visasetu.controller;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.domain.entity.TripModule;
import com.visasetu.security.SecurityUtils;
import com.visasetu.service.EncryptionService;
import com.visasetu.service.TripDraftService;
import com.visasetu.service.TripService;
import jakarta.persistence.OptimisticLockException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripDraftService tripDraftService;
    private final TripService tripService;
    private final EncryptionService encryptionService;

    @Data
    public static class CreateDraftRequest {
        private String email;
        private String nationality;
        private String categoryName;
    }

    @Data
    public static class AutosaveDraftRequest {
        private Long version;
        private String sharedProfileJson;
        private String modulesJson;
        private Integer step;
    }

    @Data
    public static class SubmitDraftRequest {
        private Long version;
        private String applicantName;
        private String arrivalDate;
        private String port;
    }

    @Data
    @Builder
    public static class TripResponseDto {
        private UUID id;
        private String referenceId;
        private String applicantName;
        private String maskedEmail;
        private String maskedPassport;
        private String nationality;
        private String categoryName;
        private String status;
        private String preparationStatus;
        private BigDecimal totalAmount;
        private String currency;
        private LocalDate arrivalDate;
        private String portOfArrival;
        private String etaNumber;
        private String qrToken;
        private Instant createdAt;
        private Instant submittedAt;
        private List<TripModule> modules;
    }

    private TripResponseDto toDto(Trip trip, List<TripModule> modules) {
        String plainPassport = (trip.getPassportNumberEncrypted() != null)
                ? encryptionService.decrypt(trip.getPassportNumberEncrypted())
                : null;

        return TripResponseDto.builder()
                .id(trip.getId())
                .referenceId(trip.getReferenceId())
                .applicantName(trip.getApplicantName())
                .maskedEmail(maskEmail(trip.getApplicantEmail()))
                .maskedPassport(EncryptionService.maskSensitive(plainPassport))
                .nationality(trip.getNationality())
                .categoryName(trip.getCategoryName())
                .status(trip.getStatus())
                .preparationStatus(trip.getPreparationStatus() != null ? trip.getPreparationStatus() : "PREPARATION_IN_PROGRESS")
                .totalAmount(trip.getTotalAmount())
                .currency(trip.getCurrency())
                .arrivalDate(trip.getArrivalDate())
                .portOfArrival(trip.getPortOfArrival())
                .etaNumber(trip.getEtaNumber())
                .qrToken(trip.getQrToken())
                .createdAt(trip.getCreatedAt())
                .submittedAt(trip.getSubmittedAt())
                .modules(modules)
                .build();
    }

    @PostMapping("/drafts")
    public ResponseEntity<?> createOrGetDraft(@RequestBody CreateDraftRequest request) {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        String email = request.getEmail();
        if ((email == null || email.isBlank()) && SecurityUtils.getCurrentUserEmail().isPresent()) {
            email = SecurityUtils.getCurrentUserEmail().get();
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required to initiate application draft."));
        }

        TripDraft draft = tripDraftService.createOrGetDraft(
                currentUserId.orElse(null),
                email,
                request.getNationality(),
                request.getCategoryName()
        );

        return ResponseEntity.ok(draft);
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<?> autosaveDraft(
            @PathVariable UUID draftId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestBody AutosaveDraftRequest request
    ) {
        try {
            Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
            TripDraft saved = (guestAccessToken != null && !guestAccessToken.isBlank())
                    ? tripDraftService.autosaveDraft(
                            draftId,
                            currentUserId.orElse(null),
                            guestAccessToken,
                            request.getVersion(),
                            request.getSharedProfileJson(),
                            request.getModulesJson(),
                            request.getStep())
                    : tripDraftService.autosaveDraft(
                            draftId,
                            currentUserId.orElse(null),
                            request.getVersion(),
                            request.getSharedProfileJson(),
                            request.getModulesJson(),
                            request.getStep());
            return ResponseEntity.ok(saved);
        } catch (AccessDeniedException ade) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
        } catch (OptimisticLockException ole) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", ole.getMessage(),
                    "code", "OPTIMISTIC_LOCK_CONFLICT"
            ));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        }
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<?> getDraft(
            @PathVariable UUID draftId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken
    ) {
        try {
            Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
            return ((guestAccessToken != null && !guestAccessToken.isBlank())
                    ? tripDraftService.getDraft(draftId, currentUserId.orElse(null), guestAccessToken)
                    : tripDraftService.getDraft(draftId, currentUserId.orElse(null)))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (AccessDeniedException ade) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
        }
    }

    @PostMapping("/drafts/{draftId}/submit")
    public ResponseEntity<?> submitDraft(
            @PathVariable UUID draftId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestBody SubmitDraftRequest request
    ) {
        try {
            Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
            Trip trip = (guestAccessToken != null && !guestAccessToken.isBlank())
                    ? tripService.submitTripFromDraft(
                            draftId,
                            currentUserId.orElse(null),
                            guestAccessToken,
                            request.getVersion(),
                            request.getApplicantName(),
                            request.getArrivalDate(),
                            request.getPort())
                    : tripService.submitTripFromDraft(
                            draftId,
                            currentUserId.orElse(null),
                            request.getVersion(),
                            request.getApplicantName(),
                            request.getArrivalDate(),
                            request.getPort());

            List<TripModule> modules = tripService.getTripModules(trip.getId());
            return ResponseEntity.ok(toDto(trip, modules));
        } catch (AccessDeniedException ade) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ade.getMessage()));
        } catch (OptimisticLockException ole) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", ole.getMessage(),
                    "code", "OPTIMISTIC_LOCK_CONFLICT"
            ));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTrips() {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }

        List<Trip> trips = tripService.getUserTrips(currentUserId.get());
        List<TripResponseDto> dtos = trips.stream()
                .map(t -> toDto(t, tripService.getTripModules(t.getId())))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{referenceId}")
    public ResponseEntity<?> getTrip(
            @PathVariable String referenceId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken
    ) {
        Optional<Trip> tripOpt = tripService.getTripByReference(referenceId.trim());
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        Trip trip = tripOpt.get();
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();

        // Enforce ownership: if trip has an owner, only the owner or an admin can access
        if (trip.getUserId() != null) {
            if (currentUserId.isEmpty() || (!trip.getUserId().equals(currentUserId.get()) && !SecurityUtils.hasRole("ADMIN"))) {
                log.warn("IDOR attempt: User {} attempted to access trip {} owned by {}",
                        currentUserId.orElse(null), referenceId, trip.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "You do not have permission to view this application."
                ));
            }
        } else if (trip.getGuestAccessToken() != null) {
            // Unowned guest application with token
            boolean isMatchingToken = guestAccessToken != null
                    && java.security.MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            guestAccessToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();

            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("IDOR attempt: User {} attempted to access unowned trip {} with invalid guest token",
                        currentUserId.orElse(null), referenceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "You do not have permission to view this application."
                ));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();

            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("IDOR attempt: User {} attempted to access unowned trip {} with mismatched email",
                        currentUserId.orElse(null), referenceId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "You do not have permission to view this application."
                ));
            }
        }

        List<TripModule> modules = tripService.getTripModules(trip.getId());
        return ResponseEntity.ok(toDto(trip, modules));
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "*@" + parts[1];
        return name.substring(0, 2) + "****" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
