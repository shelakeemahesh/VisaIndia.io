package com.visasetu.service;

import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.dto.EligibilityResponse;
import com.visasetu.dto.EligibilityRouteDto;
import com.visasetu.dto.NationalityOptionDto;
import com.visasetu.repository.VisaRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private VisaRuleRepository visaRuleRepository;

    @InjectMocks
    private EligibilityService eligibilityService;

    private VisaRule voaRule;
    private VisaRule eVisaRule;
    private VisaRule embassyRule;

    @BeforeEach
    void setUp() {
        voaRule = VisaRule.builder()
                .id(UUID.randomUUID())
                .nationality("JPN")
                .purpose(VisaPurpose.TOURISM)
                .routeType(RouteType.VISA_ON_ARRIVAL)
                .title("Visa on Arrival (VoA)")
                .description("Available for Japanese nationals arriving at 6 airports.")
                .fee(new BigDecimal("25.00"))
                .currency("USD")
                .processingDaysMin(0)
                .processingDaysMax(1)
                .validityDays(60)
                .maxStayDays(60)
                .entries("DOUBLE")
                .allowedPorts(List.of("Delhi (DEL)", "Mumbai (BOM)", "Bengaluru (BLR)"))
                .documents(List.of("Passport valid for 6 months", "Return ticket"))
                .sourceUrl("https://indianvisaonline.gov.in/visa/visa-on-arrival.html")
                .lastVerifiedAt(Instant.now())
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .isRecommended(false)
                .build();

        eVisaRule = VisaRule.builder()
                .id(UUID.randomUUID())
                .nationality("JPN")
                .purpose(VisaPurpose.TOURISM)
                .routeType(RouteType.E_VISA)
                .title("e-Tourist Visa (30 Days)")
                .description("Online electronic travel authorization.")
                .fee(new BigDecimal("25.00"))
                .currency("USD")
                .processingDaysMin(3)
                .processingDaysMax(4)
                .validityDays(30)
                .maxStayDays(30)
                .entries("DOUBLE")
                .allowedPorts(List.of("29 Designated Airports"))
                .documents(List.of("Passport scan", "Photo"))
                .sourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                .lastVerifiedAt(Instant.now())
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .isRecommended(true)
                .build();

        embassyRule = VisaRule.builder()
                .id(UUID.randomUUID())
                .nationality("JPN")
                .purpose(VisaPurpose.TOURISM)
                .routeType(RouteType.EMBASSY_VISA)
                .title("Regular Embassy Tourist Visa")
                .description("Paper visa issued by Indian Embassy.")
                .fee(new BigDecimal("40.00"))
                .currency("USD")
                .processingDaysMin(7)
                .processingDaysMax(10)
                .validityDays(180)
                .maxStayDays(90)
                .entries("MULTIPLE")
                .allowedPorts(List.of("All ICPs"))
                .documents(List.of("Physical passport", "2 Photos", "Printed application"))
                .sourceUrl("https://www.indembassy-tokyo.gov.in")
                .lastVerifiedAt(Instant.now())
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .isRecommended(false)
                .build();
    }

    @Test
    @DisplayName("Acceptance: Japan + Tourism lists every route including Visa on Arrival, e-Visa, and Embassy")
    void checkEligibility_JapanTourism_returnsAllRoutesIncludingVoA() {
        when(visaRuleRepository.findActiveRules(eq("JPN"), eq(VisaPurpose.TOURISM)))
                .thenReturn(List.of(eVisaRule, voaRule, embassyRule));

        LocalDate arrivalDate = LocalDate.now().plusDays(20);
        EligibilityResponse response = eligibilityService.checkEligibility("JPN", VisaPurpose.TOURISM, arrivalDate);

        assertThat(response).isNotNull();
        assertThat(response.getNationality()).isEqualTo("JPN");
        assertThat(response.getNationalityName()).isEqualTo("Japan");
        assertThat(response.getPurpose()).isEqualTo(VisaPurpose.TOURISM);
        assertThat(response.getRoutesFound()).isEqualTo(3);
        assertThat(response.getFallbackMessage()).isNull();

        List<EligibilityRouteDto> routes = response.getRoutes();
        assertThat(routes).hasSize(3);

        // Verify VoA route exists and contains fee and processing time
        EligibilityRouteDto voaDto = routes.stream()
                .filter(r -> r.getRouteType() == RouteType.VISA_ON_ARRIVAL)
                .findFirst()
                .orElse(null);
        assertThat(voaDto).isNotNull();
        assertThat(voaDto.getTitle()).isEqualTo("Visa on Arrival (VoA)");
        assertThat(voaDto.getFee()).isEqualByComparingTo("25.00");
        assertThat(voaDto.getCurrency()).isEqualTo("USD");
        assertThat(voaDto.getProcessingDaysMin()).isEqualTo(0);
        assertThat(voaDto.getProcessingDaysMax()).isEqualTo(1);
        assertThat(voaDto.getProcessingTimeText()).contains("Instant at designated port");
        assertThat(voaDto.getAllowedPorts()).contains("Delhi (DEL)");

        // Verify e-Visa recommended route
        EligibilityRouteDto eVisaDto = routes.stream()
                .filter(r -> r.getRouteType() == RouteType.E_VISA)
                .findFirst()
                .orElse(null);
        assertThat(eVisaDto).isNotNull();
        assertThat(eVisaDto.getIsRecommended()).isTrue();
        assertThat(eVisaDto.getProcessingTimeText()).isEqualTo("3–4 business days");
        assertThat(eVisaDto.getApplyByDate()).isEqualTo(arrivalDate.minusDays(4 + 1));

        // Verify Embassy route
        EligibilityRouteDto embassyDto = routes.stream()
                .filter(r -> r.getRouteType() == RouteType.EMBASSY_VISA)
                .findFirst()
                .orElse(null);
        assertThat(embassyDto).isNotNull();
        assertThat(embassyDto.getProcessingTimeText()).isEqualTo("7–10 business days");
    }

    @Test
    @DisplayName("Fallback: No rule found returns fallback message directing to embassy")
    void checkEligibility_noRuleFound_returnsFallbackMessage() {
        when(visaRuleRepository.findActiveRules(eq("XYZ"), any()))
                .thenReturn(Collections.emptyList());

        EligibilityResponse response = eligibilityService.checkEligibility("XYZ", VisaPurpose.TOURISM, null);

        assertThat(response).isNotNull();
        assertThat(response.getRoutesFound()).isEqualTo(0);
        assertThat(response.getRoutes()).isEmpty();
        assertThat(response.getFallbackMessage())
                .isNotBlank()
                .contains("Please contact the nearest Indian Embassy or Consulate");
    }

    @Test
    @DisplayName("Validation: Throws IllegalArgumentException when nationality or purpose is missing")
    void checkEligibility_missingInput_throwsException() {
        assertThatThrownBy(() -> eligibilityService.checkEligibility(null, VisaPurpose.TOURISM, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> eligibilityService.checkEligibility("JPN", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Supported nationalities returns list sorted alphabetically")
    void getSupportedNationalities_returnsSortedList() {
        when(visaRuleRepository.findDistinctNationalities()).thenReturn(List.of("JPN", "USA", "FRA"));

        List<NationalityOptionDto> list = eligibilityService.getSupportedNationalities();

        assertThat(list).isNotEmpty();
        assertThat(list.stream().map(NationalityOptionDto::getCode)).contains("JPN", "USA", "FRA");
    }
}
