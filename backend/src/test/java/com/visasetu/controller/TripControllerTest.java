package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.domain.entity.TripModule;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class TripControllerTest {

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
    @WithMockUser
    @DisplayName("POST /api/v1/trips/drafts initializes draft application")
    void shouldCreateOrGetDraft() throws Exception {
        UUID draftId = UUID.randomUUID();
        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-9901")
                .applicantEmail("traveler@example.com")
                .currentStep(1)
                .version(0L)
                .createdAt(Instant.now())
                .build();

        given(tripDraftService.createOrGetDraft(any(), eq("traveler@example.com"), any(), any()))
                .willReturn(draft);

        mockMvc.perform(post("/api/v1/trips/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\",\"nationality\":\"USA\",\"categoryName\":\"e-Tourist Visa (30 Days)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("VS-2026-9901"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/v1/trips/drafts/{id} autosaves progress with optimistic lock")
    void shouldAutosaveDraft() throws Exception {
        UUID draftId = UUID.randomUUID();
        TripDraft saved = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-9901")
                .applicantEmail("traveler@example.com")
                .currentStep(2)
                .version(1L)
                .sharedProfileJson("{\"firstName\":\"Jane\"}")
                .updatedAt(Instant.now())
                .build();

        given(tripDraftService.autosaveDraft(eq(draftId), any(), eq(0L), any(), any(), eq(2)))
                .willReturn(saved);

        mockMvc.perform(put("/api/v1/trips/drafts/" + draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"sharedProfileJson\":\"{\\\"firstName\\\":\\\"Jane\\\"}\",\"step\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.currentStep").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/trips/drafts/{id}/submit atomically snapshots draft into PostgreSQL")
    void shouldSubmitTripFromDraft() throws Exception {
        UUID draftId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();

        Trip trip = Trip.builder()
                .id(tripId)
                .referenceId("VS-2026-9901")
                .applicantEmail("traveler@example.com")
                .applicantName("Jane Doe")
                .nationality("USA")
                .categoryName("e-Tourist Visa (30 Days)")
                .status("SUBMITTED")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .arrivalDate(LocalDate.now().plusDays(20))
                .portOfArrival("Delhi (DEL)")
                .createdAt(Instant.now())
                .build();

        TripModule module = TripModule.builder()
                .tripId(tripId)
                .moduleType("E_VISA")
                .status("VALIDATED")
                .build();

        given(tripService.submitTripFromDraft(eq(draftId), any(), eq(1L), eq("Jane Doe"), any(), eq("Delhi (DEL)")))
                .willReturn(trip);
        given(tripService.getTripModules(tripId)).willReturn(List.of(module));

        mockMvc.perform(post("/api/v1/trips/drafts/" + draftId + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"applicantName\":\"Jane Doe\",\"arrivalDate\":\"2026-11-20\",\"port\":\"Delhi (DEL)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("VS-2026-9901"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.totalAmount").value(25.00));
    }

    @Test
    @WithMockUser(username = "attacker@example.com")
    @DisplayName("GET /api/v1/trips/{referenceId} rejects unauthorized user when trip is owned by someone else (IDOR prevention)")
    void shouldRejectCrossUserAccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .referenceId("VS-2026-OWNED")
                .userId(ownerId) // Owned by different user
                .applicantEmail("victim@example.com")
                .applicantName("Victim User")
                .nationality("JPN")
                .categoryName("e-Tourist Visa (30 Days)")
                .status("SUBMITTED")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .createdAt(Instant.now())
                .build();

        given(tripService.getTripByReference("VS-2026-OWNED")).willReturn(Optional.of(trip));

        mockMvc.perform(get("/api/v1/trips/VS-2026-OWNED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You do not have permission to view this application."));
    }
}
