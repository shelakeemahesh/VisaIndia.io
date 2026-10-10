package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.PaymentService;
import com.visasetu.service.PaymentSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

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
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("POST /api/v1/payments/create-order creates idempotent order")
    void shouldCreateIdempotentPaymentOrder() throws Exception {
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
    @DisplayName("POST /api/v1/payments/create-order rejects missing Idempotency-Key")
    void shouldRejectMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-9901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/v1/payments/resync repairs payment state")
    void shouldResyncPayment() throws Exception {
        given(paymentService.forceReconciliation("VS-2026-9901"))
                .willReturn(Map.of(
                        "status", "RECONCILED",
                        "newTripStatus", "UNDER_REVIEW",
                        "referenceId", "VS-2026-9901",
                        "message", "Payment verified and synchronized with gateway."
                ));

        mockMvc.perform(post("/api/v1/payments/resync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-9901\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONCILED"))
                .andExpect(jsonPath("$.newTripStatus").value("UNDER_REVIEW"));
    }
}
