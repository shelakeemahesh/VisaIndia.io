package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.Trip;
import com.visasetu.service.DocumentGenerationService;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private DocumentGenerationService documentGenerationService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/documents/{referenceId}/eta-pdf streams PDF certificate for permitted user")
    void shouldStreamEtaPdf() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-4011")
                .applicantName("Sarah Jane Jenkins")
                .nationality("USA")
                .categoryName("e-Tourist Visa (5 Years)")
                .status("APPROVED")
                .etaNumber("ETA-2026-881920")
                .build();

        byte[] fakePdfBytes = "%PDF-1.4 mock pdf content".getBytes();

        given(tripService.getTripByReference("VS-2026-4011")).willReturn(Optional.of(trip));
        given(documentGenerationService.generateEtaPdf(trip)).willReturn(fakePdfBytes);

        mockMvc.perform(get("/api/v1/documents/VS-2026-4011/eta-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("GET /api/v1/documents/{referenceId}/eta-pdf rejects unauthorized user when owned by someone else (IDOR)")
    void shouldRejectUnauthorizedDocumentDownload() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .referenceId("VS-2026-4011")
                .userId(ownerId) // Owned by different user
                .applicantName("Victim")
                .status("APPROVED")
                .build();

        given(tripService.getTripByReference("VS-2026-4011")).willReturn(Optional.of(trip));

        mockMvc.perform(get("/api/v1/documents/VS-2026-4011/eta-pdf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to download this document."));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/documents/{referenceId}/qr-pass streams PNG biometric pass")
    void shouldStreamQrPass() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-4011")
                .status("APPROVED")
                .qrToken("ETA:VS-2026-4011")
                .build();

        byte[] fakePngBytes = new byte[]{1, 2, 3, 4};

        given(tripService.getTripByReference("VS-2026-4011")).willReturn(Optional.of(trip));
        given(documentGenerationService.generateSignedQrToken(any())).willReturn("ETA:VS-2026-4011");
        given(documentGenerationService.generateQrCodeImage(any(), anyInt(), anyInt())).willReturn(fakePngBytes);

        mockMvc.perform(get("/api/v1/documents/VS-2026-4011/qr-pass"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/documents/{referenceId}/eta-pdf rejects non-approved application")
    void shouldRejectEtaPdfWhenNotApproved() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-PENDING")
                .status("UNDER_REVIEW")
                .applicantName("Pending Traveler")
                .build();

        given(tripService.getTripByReference("VS-2026-PENDING")).willReturn(Optional.of(trip));

        mockMvc.perform(get("/api/v1/documents/VS-2026-PENDING/eta-pdf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Official dossier / ETA PDF is only available for APPROVED applications. Current status: UNDER_REVIEW"));
    }
}
