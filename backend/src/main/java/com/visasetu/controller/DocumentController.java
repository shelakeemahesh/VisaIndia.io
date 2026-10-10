package com.visasetu.controller;

import com.visasetu.domain.entity.Trip;
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

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final TripService tripService;
    private final DocumentGenerationService documentGenerationService;

    @GetMapping("/{referenceId}/eta-pdf")
    public ResponseEntity<byte[]> downloadEtaPdf(@PathVariable String referenceId) {
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] pdfBytes = documentGenerationService.generateEtaPdf(trip);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "ETA-" + trip.getReferenceId() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{referenceId}/qr-pass")
    public ResponseEntity<byte[]> downloadQrPass(@PathVariable String referenceId) {
        Trip trip = tripService.getTripByReference(referenceId.trim())
                .orElse(null);

        if (trip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String qrContent = trip.getQrToken() != null
                ? trip.getQrToken()
                : "ETA:" + trip.getReferenceId() + "|STATUS:" + trip.getStatus();

        byte[] qrBytes = documentGenerationService.generateQrCodeImage(qrContent, 320, 320);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrBytes.length);

        return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);
    }
}
