package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.service.JwtTokenService;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.dto.EligibilityResponse;
import com.visasetu.dto.EligibilityRouteDto;
import com.visasetu.dto.NationalityOptionDto;
import com.visasetu.service.EligibilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EligibilityController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class EligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private EligibilityService eligibilityService;

    @Test
    @DisplayName("GET /api/v1/eligibility returns 200 with matching routes")
    void checkEligibility_returnsRoutes() throws Exception {
        EligibilityRouteDto route = EligibilityRouteDto.builder()
                .id(UUID.randomUUID())
                .routeType(RouteType.VISA_ON_ARRIVAL)
                .routeTypeName("Visa on Arrival (VoA)")
                .title("Visa on Arrival")
                .fee(new BigDecimal("25.00"))
                .currency("USD")
                .processingDaysMin(0)
                .processingDaysMax(1)
                .processingTimeText("Instant at designated port (0–1 days)")
                .validityDays(60)
                .maxStayDays(60)
                .entries("DOUBLE")
                .allowedPorts(List.of("Delhi (DEL)", "Mumbai (BOM)"))
                .documents(List.of("Passport", "Ticket"))
                .sourceUrl("https://indianvisaonline.gov.in")
                .isRecommended(false)
                .build();

        EligibilityResponse response = EligibilityResponse.builder()
                .nationality("JPN")
                .nationalityName("Japan")
                .purpose(VisaPurpose.TOURISM)
                .routesFound(1)
                .routes(List.of(route))
                .build();

        when(eligibilityService.checkEligibility(eq("JPN"), eq(VisaPurpose.TOURISM), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/eligibility")
                        .param("nationality", "JPN")
                        .param("purpose", "TOURISM")
                        .param("arrivalDate", LocalDate.now().plusDays(10).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.nationality").value("JPN"))
                .andExpect(jsonPath("$.nationalityName").value("Japan"))
                .andExpect(jsonPath("$.routesFound").value(1))
                .andExpect(jsonPath("$.routes[0].routeType").value("VISA_ON_ARRIVAL"))
                .andExpect(jsonPath("$.routes[0].fee").value(25.00));
    }

    @Test
    @DisplayName("GET /api/v1/eligibility/nationalities returns list of supported countries")
    void getNationalities_returnsList() throws Exception {
        when(eligibilityService.getSupportedNationalities()).thenReturn(List.of(
                NationalityOptionDto.builder().code("JPN").name("Japan").flagEmoji("🇯🇵").build(),
                NationalityOptionDto.builder().code("USA").name("United States").flagEmoji("🇺🇸").build()
        ));

        mockMvc.perform(get("/api/v1/eligibility/nationalities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("JPN"))
                .andExpect(jsonPath("$[1].code").value("USA"));
    }

    @Test
    @DisplayName("GET /api/v1/eligibility/purposes returns visa purpose categories")
    void getPurposes_returnsList() throws Exception {
        when(eligibilityService.getSupportedPurposes()).thenReturn(List.of(
                VisaPurpose.TOURISM,
                VisaPurpose.BUSINESS
        ));

        mockMvc.perform(get("/api/v1/eligibility/purposes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TOURISM"))
                .andExpect(jsonPath("$[1].code").value("BUSINESS"));
    }
}
