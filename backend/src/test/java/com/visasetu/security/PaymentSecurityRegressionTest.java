package com.visasetu.security;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.controller.PaymentController;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
public class PaymentSecurityRegressionTest {

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
    @DisplayName("Payment Security: Missing X-Razorpay-Signature header is rejected with 400")
    void shouldRejectWebhookWithoutSignature() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\",\"order_id\":\"order_test_1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Payment Security: Invalid signature is rejected by payment processor")
    void shouldRejectWebhookWithTamperedSignature() throws Exception {
        given(paymentService.processWebhook(anyString(), anyString(), any()))
                .willReturn(false);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "tampered_bad_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\",\"order_id\":\"order_test_1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("Payment Security: User cannot create order for another user's trip")
    void shouldRejectCrossUserOrderCreation() throws Exception {
        UUID victimId = UUID.randomUUID();
        Trip victimTrip = Trip.builder()
                .referenceId("VS-2026-VICTIM")
                .userId(victimId)
                .build();

        given(tripService.getTripByReference("VS-2026-VICTIM"))
                .willReturn(Optional.of(victimTrip));

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header("Idempotency-Key", "random-idem-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceId\":\"VS-2026-VICTIM\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to initiate payment for this application."));
    }
}
