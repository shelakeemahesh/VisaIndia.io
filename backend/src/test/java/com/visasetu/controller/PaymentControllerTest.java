package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.Trip;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.PaymentService;
import com.visasetu.service.PaymentSseService;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentSseService paymentSseService;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/payments/create-order creates idempotent order")
    void shouldCreateIdempotentPaymentOrder() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-9901")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .build();

        given(tripService.getTripByReference("VS-2026-9901")).willReturn(Optional.of(trip));
        given(paymentService.createPaymentOrder("VS-2026-9901", "idemp_test_key_123"))
                .willReturn(Map.of(
                        "orderId", "order_rzp_demo_1",
                        "amount", new BigDecimal("25.00"),
                        "currency", "USD",
                        "status", "CREATED",
                        "idempotencyKey", "idemp_test_key_123",
                        "referenceId", "VS-2026-9901"
                ));

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header("Idempotency-Key", "idemp_test_key_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-9901\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_rzp_demo_1"))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.idempotencyKey").value("idemp_test_key_123"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/payments/create-order rejects missing Idempotency-Key")
    void shouldRejectMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-9901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/payments/resync queries authoritative payment state")
    void shouldResyncPayment() throws Exception {
        Trip trip = Trip.builder()
                .referenceId("VS-2026-9901")
                .status("UNDER_REVIEW")
                .build();

        given(tripService.getTripByReference("VS-2026-9901")).willReturn(Optional.of(trip));
        given(paymentService.reconcilePayment("VS-2026-9901"))
                .willReturn(Map.of(
                        "status", "CONFIRMED_PAID",
                        "currentTripStatus", "UNDER_REVIEW",
                        "referenceId", "VS-2026-9901",
                        "message", "Payment has been verified and settled with the payment gateway."
                ));

        mockMvc.perform(post("/api/v1/payments/resync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-9901\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED_PAID"))
                .andExpect(jsonPath("$.currentTripStatus").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook rejects missing signature header")
    void shouldRejectWebhookWithoutSignature() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
