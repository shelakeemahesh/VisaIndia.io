package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.DemoApplication;
import com.visasetu.repository.DemoApplicationRepository;
import com.visasetu.repository.TripRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
import com.visasetu.service.PaymentService;
import com.visasetu.service.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationTrackingController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class ApplicationTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoApplicationRepository demoApplicationRepository;

    @MockBean
    private TripRepository tripRepository;

    @MockBean
    private OtpService otpService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RateLimitingService rateLimitingService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @WithMockUser
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

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(tripRepository.findByReferenceIdIgnoreCase("VS-2026-4011")).willReturn(Optional.empty());
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
    @WithMockUser
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

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(tripRepository.findByReferenceIdIgnoreCase("VS-2026-4011")).willReturn(Optional.empty());
        given(demoApplicationRepository.findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase("VS-2026-4011", "sarah.jenkins@example.com"))
                .willReturn(Optional.of(app));
        given(otpService.verifyOtp("sarah.jenkins@example.com", "654321", "TRACKING")).willReturn(true);

        mockMvc.perform(post("/api/v1/applications/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-4011\",\"email\":\"sarah.jenkins@example.com\",\"otp\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("VS-2026-4011"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/applications/resync-payment calls authoritative payment reconciliation")
    void shouldResyncPaymentSuccessfully() throws Exception {
        given(paymentService.reconcilePayment("VS-2026-6134")).willReturn(Map.of(
                "status", "RECONCILED",
                "newTripStatus", "UNDER_REVIEW",
                "referenceId", "VS-2026-6134",
                "message", "Payment verified directly against Razorpay gateway."
        ));

        mockMvc.perform(post("/api/v1/applications/resync-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-6134\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONCILED"))
                .andExpect(jsonPath("$.newTripStatus").value("UNDER_REVIEW"));
    }
}
