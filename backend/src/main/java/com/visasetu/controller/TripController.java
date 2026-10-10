package com.visasetu.controller;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.domain.entity.TripModule;
import com.visasetu.service.TripDraftService;
import com.visasetu.service.TripService;
import jakarta.persistence.OptimisticLockException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripDraftService tripDraftService;
    private final TripService tripService;

    @Data
    public static class CreateDraftRequest {
        private String email;
        private String nationality = "USA";
        private String categoryName = "e-Tourist Visa (30 Days)";
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

    @PostMapping("/drafts")
    public ResponseEntity<?> createOrGetDraft(
            @RequestBody CreateDraftRequest request,
            java.security.Principal principal
    ) {
        String email = request.getEmail();
        if ((email == null || email.isBlank()) && principal != null) {
            email = principal.getName();
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required to initiate application draft."));
        }

        TripDraft draft = tripDraftService.createOrGetDraft(
                email,
                request.getNationality(),
                request.getCategoryName()
        );

        return ResponseEntity.ok(draft);
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<?> autosaveDraft(
            @PathVariable UUID draftId,
            @RequestBody AutosaveDraftRequest request
    ) {
        try {
            TripDraft saved = tripDraftService.autosaveDraft(
                    draftId,
                    request.getVersion(),
                    request.getSharedProfileJson(),
                    request.getModulesJson(),
                    request.getStep()
            );
            return ResponseEntity.ok(saved);
        } catch (OptimisticLockException ole) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", ole.getMessage(),
                    "code", "OPTIMISTIC_LOCK_CONFLICT"
            ));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", iae.getMessage()));
        }
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<?> getDraft(@PathVariable UUID draftId) {
        return tripDraftService.getDraft(draftId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/drafts/{draftId}/submit")
    public ResponseEntity<?> submitDraft(
            @PathVariable UUID draftId,
            @RequestBody SubmitDraftRequest request
    ) {
        try {
            Trip trip = tripService.submitTripFromDraft(
                    draftId,
                    request.getVersion(),
                    request.getApplicantName(),
                    request.getArrivalDate(),
                    request.getPort()
            );

            List<TripModule> modules = tripService.getTripModules(trip.getId());

            return ResponseEntity.ok(Map.of(
                    "tripId", trip.getId(),
                    "referenceId", trip.getReferenceId(),
                    "status", trip.getStatus(),
                    "amount", trip.getTotalAmount(),
                    "currency", trip.getCurrency(),
                    "applicantName", trip.getApplicantName(),
                    "modules", modules
            ));
        } catch (OptimisticLockException ole) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", ole.getMessage(),
                    "code", "OPTIMISTIC_LOCK_CONFLICT"
            ));
        }
    }

    @GetMapping("/{referenceId}")
    public ResponseEntity<?> getTrip(@PathVariable String referenceId) {
        return tripService.getTripByReference(referenceId)
                .map(t -> {
                    List<TripModule> modules = tripService.getTripModules(t.getId());
                    return ResponseEntity.ok(Map.of(
                            "trip", t,
                            "modules", modules
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
