package com.visasetu.service;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.domain.entity.TripModule;
import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.repository.TripDraftRepository;
import com.visasetu.repository.TripModuleRepository;
import com.visasetu.repository.TripRepository;
import com.visasetu.repository.VisaRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripModuleRepository tripModuleRepository;

    @Mock
    private TripDraftRepository tripDraftRepository;

    @Mock
    private VisaRuleRepository visaRuleRepository;

    @Mock
    private EncryptionService encryptionService;

    private TripService tripService;

    @BeforeEach
    void setUp() {
        tripService = new TripService(
                tripRepository,
                tripModuleRepository,
                tripDraftRepository,
                visaRuleRepository,
                encryptionService
        );
    }

    @Test
    @DisplayName("submitTripFromDraft rejects past arrival date")
    void submitTripFromDraft_pastArrivalDate_throwsException() {
        UUID draftId = UUID.randomUUID();
        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-TEST1")
                .isSubmitted(false)
                .nationality("USA")
                .categoryName("e-Tourist Visa (30 Days)")
                .sharedProfileJson("{\"fullName\":\"Alice Johnson\",\"passportNumber\":\"L1234567\",\"dob\":\"1990-05-15\"}")
                .modulesJson("{\"eSuvidha\":{\"healthDeclarationAccepted\":true}}")
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.submitTripFromDraft(
                draftId, 0L, "Alice Johnson", LocalDate.now().minusDays(1).toString(), "DEL"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Arrival date must be in the future");
    }

    @Test
    @DisplayName("submitTripFromDraft rejects future date of birth")
    void submitTripFromDraft_futureDob_throwsException() {
        UUID draftId = UUID.randomUUID();
        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-TEST2")
                .isSubmitted(false)
                .nationality("USA")
                .categoryName("e-Tourist Visa (30 Days)")
                .sharedProfileJson("{\"fullName\":\"Alice Johnson\",\"passportNumber\":\"L1234567\",\"dob\":\"2030-01-01\"}")
                .modulesJson("{\"eSuvidha\":{\"healthDeclarationAccepted\":true}}")
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.submitTripFromDraft(
                draftId, 0L, "Alice Johnson", LocalDate.now().plusDays(20).toString(), "DEL"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Date of birth must be in the past");
    }

    @Test
    @DisplayName("submitTripFromDraft rejects passport expiry less than 6 months after arrival")
    void submitTripFromDraft_shortPassportExpiry_throwsException() {
        UUID draftId = UUID.randomUUID();
        LocalDate arrival = LocalDate.now().plusDays(20);
        LocalDate expiredPassport = arrival.plusDays(90); // Only 3 months validity

        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-TEST3")
                .isSubmitted(false)
                .nationality("USA")
                .categoryName("e-Tourist Visa (30 Days)")
                .sharedProfileJson("{\"fullName\":\"Alice Johnson\",\"passportNumber\":\"L1234567\",\"dob\":\"1990-01-01\",\"passportExpiry\":\"" + expiredPassport + "\"}")
                .modulesJson("{}")
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.submitTripFromDraft(
                draftId, 0L, "Alice Johnson", arrival.toString(), "DEL"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Passport must be valid for at least 6 months (180 days)");
    }

    @Test
    @DisplayName("submitTripFromDraft matches Business visa category and purpose dynamically")
    void submitTripFromDraft_matchesBusinessCategory() {
        UUID draftId = UUID.randomUUID();
        LocalDate arrival = LocalDate.now().plusDays(25);
        LocalDate passportExpiry = arrival.plusDays(365);

        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-BIZ1")
                .isSubmitted(false)
                .nationality("USA")
                .categoryName("e-Business Visa")
                .sharedProfileJson("{\"fullName\":\"Bob Builder\",\"passportNumber\":\"B9988776\",\"dob\":\"1985-06-20\",\"passportExpiry\":\"" + passportExpiry + "\"}")
                .modulesJson("{\"eSuvidha\":{\"healthDeclarationAccepted\":true}}")
                .build();

        VisaRule bizRule = VisaRule.builder()
                .nationality("USA")
                .purpose(VisaPurpose.BUSINESS)
                .routeType(RouteType.E_VISA)
                .title("e-Business Visa")
                .fee(new BigDecimal("80.00"))
                .currency("USD")
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(draft));
        given(visaRuleRepository.findActiveRules("USA", VisaPurpose.BUSINESS)).willReturn(List.of(bizRule));
        given(encryptionService.encrypt(any())).willReturn("encrypted-pii");

        Trip savedTripMock = Trip.builder()
                .id(UUID.randomUUID())
                .referenceId("VS-2026-BIZ1")
                .status("SUBMITTED")
                .totalAmount(new BigDecimal("80.00"))
                .currency("USD")
                .build();
        given(tripRepository.save(any(Trip.class))).willReturn(savedTripMock);

        Trip result = tripService.submitTripFromDraft(draftId, 0L, "Bob Builder", arrival.toString(), "Mumbai (BOM)");

        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("80.00"));
        assertThat(result.getCurrency()).isEqualTo("USD");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TripModule>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripModuleRepository).saveAll(captor.capture());
        List<TripModule> savedModules = captor.getValue();
        assertThat(savedModules).hasSize(3);

        TripModule suvidha = savedModules.stream().filter(m -> "E_SUVIDHA".equals(m.getModuleType())).findFirst().orElseThrow();
        assertThat(suvidha.getStatus()).isEqualTo("VALIDATED");
        assertThat(suvidha.getFormDataJson()).contains("\"healthDeclarationAccepted\":true");
    }

    @Test
    @DisplayName("submitTripFromDraft sets PENDING_CONSENT when health declaration not explicitly accepted")
    void submitTripFromDraft_missingHealthConsent_setsPendingConsent() {
        UUID draftId = UUID.randomUUID();
        LocalDate arrival = LocalDate.now().plusDays(25);

        TripDraft draft = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-NOCONSENT")
                .isSubmitted(false)
                .nationality("USA")
                .categoryName("e-Tourist Visa (30 Days)")
                .sharedProfileJson("{\"fullName\":\"Charlie Brown\",\"passportNumber\":\"C1122334\",\"dob\":\"1992-03-10\"}")
                .modulesJson("{\"eSuvidha\":{\"healthDeclarationAccepted\":false}}")
                .build();

        VisaRule touristRule = VisaRule.builder()
                .nationality("USA")
                .purpose(VisaPurpose.TOURISM)
                .routeType(RouteType.E_VISA)
                .title("e-Tourist Visa (30 Days)")
                .fee(new BigDecimal("25.00"))
                .currency("USD")
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(draft));
        given(visaRuleRepository.findActiveRules("USA", VisaPurpose.TOURISM)).willReturn(List.of(touristRule));
        given(encryptionService.encrypt(any())).willReturn("encrypted-pii");

        Trip savedTripMock = Trip.builder()
                .id(UUID.randomUUID())
                .referenceId("VS-2026-NOCONSENT")
                .status("SUBMITTED")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .build();
        given(tripRepository.save(any(Trip.class))).willReturn(savedTripMock);

        tripService.submitTripFromDraft(draftId, 0L, "Charlie Brown", arrival.toString(), "DEL");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TripModule>> captor = ArgumentCaptor.forClass(List.class);
        verify(tripModuleRepository).saveAll(captor.capture());
        List<TripModule> savedModules = captor.getValue();

        TripModule suvidha = savedModules.stream().filter(m -> "E_SUVIDHA".equals(m.getModuleType())).findFirst().orElseThrow();
        assertThat(suvidha.getStatus()).isEqualTo("PENDING_CONSENT");
        assertThat(suvidha.getFormDataJson()).contains("\"healthDeclarationAccepted\":false");
    }
}
