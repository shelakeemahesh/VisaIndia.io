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
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<?> downloadEtaPdf(
            @PathVariable String referenceId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestParam(value = "token", required = false) String guestTokenParam
    ) {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        // Must be in APPROVED status
        if (!"APPROVED".equalsIgnoreCase(trip.getStatus())) {
            log.warn("Unauthorized attempt to download ETA PDF for application {} with non-approved status {}", referenceId, trip.getStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Official dossier / ETA PDF is only available for APPROVED applications. Current status: " + trip.getStatus()
            ));
        }

        // Ownership and authorization check
        if (trip.getUserId() != null) {
            if (currentUserId.isEmpty() || (!trip.getUserId().equals(currentUserId.get()) && !SecurityUtils.hasRole("ADMIN"))) {
                log.warn("Unauthorized document download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this document."));
            }
        } else if (trip.getGuestAccessToken() != null) {
            String guestToken = guestAccessToken != null ? guestAccessToken : guestTokenParam;
            boolean isMatchingToken = guestToken != null
                    && MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(StandardCharsets.UTF_8),
                            guestToken.trim().getBytes(StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();

            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("Unauthorized guest document download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
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
        headers.setContentDispositionFormData("attachment", "Dossier-" + trip.getReferenceId() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{referenceId}/qr-pass")
    public ResponseEntity<?> downloadQrPass(
            @PathVariable String referenceId,
            @RequestHeader(value = "X-Guest-Access-Token", required = false) String guestAccessToken,
            @RequestParam(value = "token", required = false) String guestTokenParam
    ) {
        Optional<UUID> currentUserId = SecurityUtils.getCurrentUserId();
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Application not found."));
        }

        // Must be in APPROVED status
        if (!"APPROVED".equalsIgnoreCase(trip.getStatus())) {
            log.warn("Unauthorized attempt to download QR pass for application {} with non-approved status {}", referenceId, trip.getStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "QR entry pass is only available for APPROVED applications. Current status: " + trip.getStatus()
            ));
        }

        // Ownership and authorization check
        if (trip.getUserId() != null) {
            if (currentUserId.isEmpty() || (!trip.getUserId().equals(currentUserId.get()) && !SecurityUtils.hasRole("ADMIN"))) {
                log.warn("Unauthorized QR pass download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You do not have permission to download this QR pass."));
            }
        } else if (trip.getGuestAccessToken() != null) {
            String guestToken = guestAccessToken != null ? guestAccessToken : guestTokenParam;
            boolean isMatchingToken = guestToken != null
                    && MessageDigest.isEqual(
                            trip.getGuestAccessToken().getBytes(StandardCharsets.UTF_8),
                            guestToken.trim().getBytes(StandardCharsets.UTF_8));
            boolean isMatchingEmail = SecurityUtils.getCurrentUserEmail()
                    .filter(email -> trip.getApplicantEmail() != null && trip.getApplicantEmail().equalsIgnoreCase(email))
                    .isPresent();

            if (!isMatchingToken && !isMatchingEmail && !SecurityUtils.hasRole("ADMIN")) {
                log.warn("Unauthorized guest QR pass download attempt for reference {} by user {}", referenceId, currentUserId.orElse(null));
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

        String qrContent = documentGenerationService.generateSignedQrToken(trip);
        if (qrContent == null || qrContent.isBlank()) {
            qrContent = trip.getQrToken() != null ? trip.getQrToken() : "DOSSIER:" + trip.getReferenceId() + "|STATUS:" + trip.getStatus();
        }
        byte[] qrBytes = documentGenerationService.generateQrCodeImage(qrContent, 320, 320);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrBytes.length);
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);
    }
}
