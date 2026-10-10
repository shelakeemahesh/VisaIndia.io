package com.visasetu.security;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.controller.TripController;
import com.visasetu.domain.entity.Trip;
import com.visasetu.service.EncryptionService;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.TripDraftService;
import com.visasetu.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
public class IdorSecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripDraftService tripDraftService;

    @MockBean
    private TripService tripService;

    @MockBean
    private EncryptionService encryptionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("IDOR Security: Attacker cannot read Victim's draft by UUID")
    void shouldDenyCrossUserDraftRead() throws Exception {
        UUID draftId = UUID.randomUUID();
        given(tripDraftService.getDraft(eq(draftId), any()))
                .willThrow(new AccessDeniedException("You do not have permission to access this draft."));

        mockMvc.perform(get("/api/v1/trips/drafts/" + draftId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to access this draft."));
    }

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("IDOR Security: Attacker cannot autosave or mutate Victim's draft")
    void shouldDenyCrossUserDraftMutation() throws Exception {
        UUID draftId = UUID.randomUUID();
        given(tripDraftService.autosaveDraft(eq(draftId), any(), any(), any(), any(), any()))
                .willThrow(new AccessDeniedException("You do not have permission to modify this draft."));

        mockMvc.perform(put("/api/v1/trips/drafts/" + draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"sharedProfileJson\":\"{}\",\"step\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to modify this draft."));
    }

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("IDOR Security: Attacker cannot submit Victim's draft")
    void shouldDenyCrossUserDraftSubmission() throws Exception {
        UUID draftId = UUID.randomUUID();
        given(tripService.submitTripFromDraft(eq(draftId), any(), any(), any(), any(), any()))
                .willThrow(new AccessDeniedException("You do not have permission to submit this application."));

        mockMvc.perform(post("/api/v1/trips/drafts/" + draftId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"applicantName\":\"Hacker\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to submit this application."));
    }

    @Test
    @WithMockUser(username = "legit.user@example.com")
    @DisplayName("Integrity Security: Duplicate submission returns 409 Conflict")
    void shouldRejectDuplicateSubmission() throws Exception {
        UUID draftId = UUID.randomUUID();
        given(tripService.submitTripFromDraft(eq(draftId), any(), any(), any(), any(), any()))
                .willThrow(new IllegalStateException("Draft has already been submitted as an application."));

        mockMvc.perform(post("/api/v1/trips/drafts/" + draftId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"applicantName\":\"Legit User\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Draft has already been submitted as an application."));
    }
}
