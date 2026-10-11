package com.visasetu.controller;

import com.visasetu.domain.entity.Trip;
import com.visasetu.security.SecurityUtils;
import com.visasetu.service.DocumentGenerationService;
import com.visasetu.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final TripService tripService;
    private final DocumentGenerationService documentGenerationService;

    @GetMapping("/{referenceId}/eta-pdf")
    public ResponseEntity<?> downloadEtaPdf(@PathVariable String referenceId) {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        // Ownership and authorization check
        if (trip.getUserId() != null) {
            if (currentUserId.isEmpty() || (!trip.getUserId().equals(currentUserId.get()) && !SecurityUtils.hasRole("ADMIN"))) {
                log.warn("Unauthorized document download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this document."));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("Unauthorized guest document download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this document."));
            }
        }

        byte[] pdfBytes = documentGenerationService.generateEtaPdf(trip);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ETA-" + trip.getReferenceId() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{referenceId}/qr-pass")
    public ResponseEntity<?> downloadQrPass(@PathVariable String referenceId) {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        // Ownership and authorization check
        if (trip.getUserId() != null) {
            if (currentUserId.isEmpty() || (!trip.getUserId().equals(currentUserId.get()) && !SecurityUtils.hasRole("ADMIN"))) {
                log.warn("Unauthorized QR pass download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this QR pass."));
            }
        } else if (trip.getApplicantEmail() != null) {
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();
            if (!isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("Unauthorized guest QR pass download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this QR pass."));
            }
        }

        String qrContent = trip.getQrToken() != null
                ? trip.getQrToken()
                : "ETA:" + trip.getReferenceId() + "|STATUS:" + trip.getStatus();

        byte[] qrBytes = documentGenerationService.generateQrCodeImage(qrContent, 320, 320);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrBytes.length);
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);
    }
}
