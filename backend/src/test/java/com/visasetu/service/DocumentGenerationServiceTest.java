package com.visasetu.service;

import com.visasetu.domain.entity.Trip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentGenerationServiceTest {

    private final DocumentGenerationService service = new DocumentGenerationService();

    @Test
    @DisplayName("generateEtaPdf returns valid non-empty PDF byte stream")
    void generateEtaPdf_returnsValidPdfBytes() {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-7890")
                .etaNumber("ETA-2026-IND-99999")
                .applicantName("Kenji Sato")
                .nationality("JPN")
                .categoryName("e-Tourist Visa (30 Days)")
                .arrivalDate(LocalDate.now().plusDays(10))
                .portOfArrival("Delhi (DEL)")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .build();

        byte[] pdfBytes = service.generateEtaPdf(trip);

        assertThat(pdfBytes).isNotEmpty();
        // PDF magic bytes: %PDF-
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length));
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateQrPass returns valid PNG image byte stream")
    void generateQrPass_returnsValidPngBytes() {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-7890")
                .etaNumber("ETA-2026-IND-99999")
                .applicantName("Kenji Sato")
                .nationality("JPN")
                .build();

        byte[] pngBytes = service.generateQrCodeImage("ETA:VS-2026-7890", 300, 300);

        assertThat(pngBytes).isNotEmpty();
        // PNG magic bytes: 0x89 'P' 'N' 'G'
        assertThat(pngBytes[0]).isEqualTo((byte) 0x89);
        assertThat(pngBytes[1]).isEqualTo((byte) 'P');
        assertThat(pngBytes[2]).isEqualTo((byte) 'N');
        assertThat(pngBytes[3]).isEqualTo((byte) 'G');
    }
}
