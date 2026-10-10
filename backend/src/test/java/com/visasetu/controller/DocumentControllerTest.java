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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

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
    @DisplayName("GET /api/v1/documents/{referenceId}/eta-pdf streams PDF certificate")
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
                .andExpect(header().string("Content-Disposition", "form-data; name=\"inline\"; filename=\"ETA-VS-2026-4011.pdf\""));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{referenceId}/qr-pass streams PNG biometric pass")
    void shouldStreamQrPass() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-4011")
                .status("APPROVED")
                .qrToken("ETA:VS-2026-4011")
                .build();

        byte[] fakePngBytes = new byte[]{1, 2, 3, 4};

        given(tripService.getTripByReference("VS-2026-4011")).willReturn(Optional.of(trip));
        given(documentGenerationService.generateQrCodeImage(anyString(), anyInt(), anyInt())).willReturn(fakePngBytes);

        mockMvc.perform(get("/api/v1/documents/VS-2026-4011/qr-pass"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}
