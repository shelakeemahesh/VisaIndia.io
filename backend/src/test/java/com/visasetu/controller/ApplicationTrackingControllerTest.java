package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.DemoApplication;
import com.visasetu.repository.DemoApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationTrackingController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class})
class ApplicationTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoApplicationRepository demoApplicationRepository;

    @Test
    @DisplayName("POST /api/v1/applications/track returns OTP_REQUIRED when otp is omitted")
    void shouldReturnOtpRequired() throws Exception {
        DemoApplication app = DemoApplication.builder()
                .referenceId("VS-2026-4011")
                .applicantEmail("sarah.jenkins@example.com")
                .applicantName("Sarah Jane Jenkins")
                .nationality("USA")
                .categoryName("e-Tourist Visa (5 Years)")
                .status("APPROVED")
                .amount(new BigDecimal("80.00"))
                .arrivalDate(LocalDate.now().plusDays(10))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(demoApplicationRepository.findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase("VS-2026-4011", "sarah.jenkins@example.com"))
                .willReturn(Optional.of(app));

        mockMvc.perform(post("/api/v1/applications/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-4011\",\"email\":\"sarah.jenkins@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_REQUIRED"))
                .andExpect(jsonPath("$.referenceId").value("VS-2026-4011"));
    }

    @Test
    @DisplayName("POST /api/v1/applications/track returns details and timeline when valid otp provided")
    void shouldReturnApplicationDetailsWithOtp() throws Exception {
        DemoApplication app = DemoApplication.builder()
                .referenceId("VS-2026-4011")
                .applicantEmail("sarah.jenkins@example.com")
                .applicantName("Sarah Jane Jenkins")
                .nationality("USA")
                .categoryName("e-Tourist Visa (5 Years)")
                .status("APPROVED")
                .amount(new BigDecimal("80.00"))
                .arrivalDate(LocalDate.now().plusDays(10))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(demoApplicationRepository.findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase("VS-2026-4011", "sarah.jenkins@example.com"))
                .willReturn(Optional.of(app));

        mockMvc.perform(post("/api/v1/applications/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-4011\",\"email\":\"sarah.jenkins@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("VS-2026-4011"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/applications/resync-payment repairs pending payment")
    void shouldResyncPaymentSuccessfully() throws Exception {
        DemoApplication app = DemoApplication.builder()
                .referenceId("VS-2026-6134")
                .applicantEmail("david.kim@example.com")
                .applicantName("David Kim")
                .status("PAYMENT_PENDING")
                .amount(new BigDecimal("80.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(demoApplicationRepository.findByReferenceIdIgnoreCase("VS-2026-6134"))
                .willReturn(Optional.of(app));
        given(demoApplicationRepository.save(any(DemoApplication.class))).willReturn(app);

        mockMvc.perform(post("/api/v1/applications/resync-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-6134\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONCILED"))
                .andExpect(jsonPath("$.newStatus").value("UNDER_REVIEW"));
    }
}
