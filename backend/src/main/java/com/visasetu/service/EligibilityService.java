package com.visasetu.service;

import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.dto.EligibilityResponse;
import com.visasetu.dto.EligibilityRouteDto;
import com.visasetu.dto.NationalityOptionDto;
import com.visasetu.repository.VisaRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {

    private final VisaRuleRepository visaRuleRepository;

    private static final Map<String, CountryInfo> COUNTRY_REGISTRY = Map.ofEntries(
            Map.entry("JPN", new CountryInfo("Japan", "🇯🇵")),
            Map.entry("USA", new CountryInfo("United States", "🇺🇸")),
            Map.entry("GBR", new CountryInfo("United Kingdom", "🇬🇧")),
            Map.entry("DEU", new CountryInfo("Germany", "🇩🇪")),
            Map.entry("FRA", new CountryInfo("France", "🇫🇷")),
            Map.entry("KOR", new CountryInfo("South Korea", "🇰🇷")),
            Map.entry("ITA", new CountryInfo("Italy", "🇮🇹")),
            Map.entry("BRA", new CountryInfo("Brazil", "🇧🇷")),
            Map.entry("NPL", new CountryInfo("Nepal", "🇳🇵")),
            Map.entry("BTN", new CountryInfo("Bhutan", "🇧🇹")),
            Map.entry("CAN", new CountryInfo("Canada", "🇨🇦")),
            Map.entry("AUS", new CountryInfo("Australia", "🇦🇺")),
            Map.entry("SGP", new CountryInfo("Singapore", "🇸🇬")),
            Map.entry("ARE", new CountryInfo("United Arab Emirates", "🇦🇪"))
    );

    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(String nationality, VisaPurpose purpose, LocalDate arrivalDate) {
        if (nationality == null || nationality.isBlank() || purpose == null) {
            throw new IllegalArgumentException("Nationality and purpose must be provided");
        }

        String normalizedNationality = nationality.trim().toUpperCase();
        CountryInfo country = COUNTRY_REGISTRY.getOrDefault(
                normalizedNationality,
                new CountryInfo(normalizedNationality, "🌐")
        );

        List<VisaRule> rules = visaRuleRepository.findActiveRules(normalizedNationality, purpose);

        if (rules.isEmpty()) {
            return EligibilityResponse.builder()
                    .nationality(normalizedNationality)
                    .nationalityName(country.name)
                    .purpose(purpose)
                    .arrivalDate(arrivalDate)
                    .routesFound(0)
                    .routes(Collections.emptyList())
                    .fallbackMessage("No direct online or VoA route found for nationality " + country.name +
                            " and purpose " + purpose.getDisplayName() +
                            ". Please contact the nearest Indian Embassy or Consulate.")
                    .generalDisclaimer("Sample data — verify with official source.")
                    .build();
        }

        List<EligibilityRouteDto> routeDtos = rules.stream()
                .map(rule -> toDto(rule, arrivalDate))
                .toList();

        return EligibilityResponse.builder()
                .nationality(normalizedNationality)
                .nationalityName(country.name)
                .purpose(purpose)
                .arrivalDate(arrivalDate)
                .routesFound(routeDtos.size())
                .routes(routeDtos)
                .fallbackMessage(null)
                .generalDisclaimer("Concept project — not affiliated with the Government of India. Sample data — verify with official source.")
                .build();
    }

    @Transactional(readOnly = true)
    public List<NationalityOptionDto> getSupportedNationalities() {
        List<String> dbCodes = visaRuleRepository.findDistinctNationalities();
        Set<String> allCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allCodes.addAll(dbCodes);
        allCodes.addAll(COUNTRY_REGISTRY.keySet());

        List<NationalityOptionDto> options = new ArrayList<>();
        for (String code : allCodes) {
            CountryInfo info = COUNTRY_REGISTRY.getOrDefault(code.toUpperCase(), new CountryInfo(code.toUpperCase(), "🌐"));
            options.add(NationalityOptionDto.builder()
                    .code(code.toUpperCase())
                    .name(info.name)
                    .flagEmoji(info.flagEmoji)
                    .build());
        }

        options.sort(Comparator.comparing(NationalityOptionDto::getName));
        return options;
    }

    public List<VisaPurpose> getSupportedPurposes() {
        return Arrays.asList(VisaPurpose.values());
    }

    private EligibilityRouteDto toDto(VisaRule rule, LocalDate arrivalDate) {
        String processingTimeText;
        if (rule.getProcessingDaysMin() == 0 && rule.getProcessingDaysMax() <= 1) {
            processingTimeText = "Instant at designated port (0–1 days)";
        } else if (rule.getProcessingDaysMin().equals(rule.getProcessingDaysMax())) {
            processingTimeText = rule.getProcessingDaysMin() + " business days";
        } else {
            processingTimeText = rule.getProcessingDaysMin() + "–" + rule.getProcessingDaysMax() + " business days";
        }

        LocalDate applyByDate = null;
        if (arrivalDate != null) {
            applyByDate = arrivalDate.minusDays(rule.getProcessingDaysMax() + 1L);
        }

        return EligibilityRouteDto.builder()
                .id(rule.getId())
                .routeType(rule.getRouteType())
                .routeTypeName(rule.getRouteType().getDisplayName())
                .title(rule.getTitle())
                .description(rule.getDescription())
                .fee(rule.getFee())
                .currency(rule.getCurrency())
                .processingDaysMin(rule.getProcessingDaysMin())
                .processingDaysMax(rule.getProcessingDaysMax())
                .processingTimeText(processingTimeText)
                .validityDays(rule.getValidityDays())
                .maxStayDays(rule.getMaxStayDays())
                .entries(rule.getEntries())
                .allowedPorts(rule.getAllowedPorts())
                .documents(rule.getDocuments())
                .sourceUrl(rule.getSourceUrl())
                .lastVerifiedAt(rule.getLastVerifiedAt())
                .applyByDate(applyByDate)
                .isRecommended(rule.getIsRecommended())
                .dataDisclaimer("Sample data — verify with official source")
                .build();
    }

    private record CountryInfo(String name, String flagEmoji) {}
}
