package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.HelpTicket;
import com.visasetu.repository.HelpTicketRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HelpTicketController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtTokenService.class})
class HelpTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HelpTicketRepository helpTicketRepository;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Test
    @DisplayName("POST /api/v1/help/report-problem persists valid problem report")
    void shouldPersistProblemReport() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);

        mockMvc.perform(post("/api/v1/help/report-problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "PAYMENT",
                                  "subject": "Payment transaction pending verification",
                                  "description": "Card was charged but status did not advance immediately",
                                  "email": "traveler@example.com",
                                  "referenceId": "VS-2026-6134"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.ticketId").exists())
                .andExpect(jsonPath("$.category").value("PAYMENT"));

        verify(helpTicketRepository).save(any(HelpTicket.class));
    }

    @Test
    @DisplayName("POST /api/v1/help/report-problem rejects invalid email")
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/help/report-problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "PAYMENT",
                                  "subject": "Payment issue",
                                  "description": "Card charged",
                                  "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
