package com.visasetu.config;

import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.repository.VisaRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader {

    private final VisaRuleRepository visaRuleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedDataIfEmpty() {
        if (visaRuleRepository.count() > 0) {
            log.info("Visa rules already present in database (count: {})", visaRuleRepository.count());
            return;
        }

        log.info("Seeding initial visa rules into database...");

        List<VisaRule> sampleRules = List.of(
                // JAPAN (JPN) - TOURISM (Acceptance Criteria)
                VisaRule.builder()
                        .nationality("JPN")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.VISA_ON_ARRIVAL)
                        .title("Visa on Arrival (VoA)")
                        .description("Available for Japanese nationals arriving at 6 designated international airports. Sample data — verify with official source.")
                        .fee(new BigDecimal("25.00"))
                        .currency("USD")
                        .processingDaysMin(0)
                        .processingDaysMax(1)
                        .validityDays(60)
                        .maxStayDays(60)
                        .entries("DOUBLE")
                        .allowedPorts(List.of("Delhi (DEL)", "Mumbai (BOM)", "Kolkata (CCU)", "Chennai (MAA)", "Bengaluru (BLR)", "Hyderabad (HYD)"))
                        .documents(List.of("Passport with minimum 6 months validity", "Confirmed return/onward flight ticket", "Sufficient financial means for stay", "Completed VoA application form"))
                        .sourceUrl("https://indianvisaonline.gov.in/visa/visa-on-arrival.html")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(false)
                        .build(),

                VisaRule.builder()
                        .nationality("JPN")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.E_VISA)
                        .title("e-Tourist Visa (30 Days)")
                        .description("Fast electronic visa for recreation, sightseeing, and casual visits. Sample data — verify with official source.")
                        .fee(new BigDecimal("25.00"))
                        .currency("USD")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .validityDays(30)
                        .maxStayDays(30)
                        .entries("DOUBLE")
                        .allowedPorts(List.of("29 Designated Airports (DEL, BOM, BLR, etc.)", "5 Major Seaports"))
                        .documents(List.of("Recent front-facing color photo (JPEG, 10KB-1MB)", "Passport biographical page copy (PDF/JPEG, 10KB-300KB)"))
                        .sourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(true)
                        .build(),

                VisaRule.builder()
                        .nationality("JPN")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.E_VISA)
                        .title("e-Tourist Visa (1 Year)")
                        .description("Long-term electronic tourist visa allowing multiple entries throughout one year. Sample data — verify with official source.")
                        .fee(new BigDecimal("40.00"))
                        .currency("USD")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .validityDays(365)
                        .maxStayDays(90)
                        .entries("MULTIPLE")
                        .allowedPorts(List.of("29 Designated Airports", "5 Major Seaports"))
                        .documents(List.of("Recent front-facing color photo", "Passport biographical page scan"))
                        .sourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(false)
                        .build(),

                VisaRule.builder()
                        .nationality("JPN")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.EMBASSY_VISA)
                        .title("Regular Embassy Visa (Tourist)")
                        .description("Physical sticker visa stamped at the Embassy of India in Tokyo or Consulate in Osaka. Sample data — verify with official source.")
                        .fee(new BigDecimal("40.00"))
                        .currency("USD")
                        .processingDaysMin(7)
                        .processingDaysMax(10)
                        .validityDays(180)
                        .maxStayDays(90)
                        .entries("MULTIPLE")
                        .allowedPorts(List.of("All authorized Indian Immigration Check Posts"))
                        .documents(List.of("Physical passport with at least 2 blank pages", "Printed online application form with signature", "2 passport photos (5cm x 5cm)", "Proof of address"))
                        .sourceUrl("https://www.indembassy-tokyo.gov.in/visa_services.html")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(false)
                        .build(),

                // UNITED STATES (USA) - TOURISM
                VisaRule.builder()
                        .nationality("USA")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.E_VISA)
                        .title("e-Tourist Visa (1 Year)")
                        .description("1-Year Multiple Entry electronic visa for US citizens. Sample data — verify with official source.")
                        .fee(new BigDecimal("40.00"))
                        .currency("USD")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .validityDays(365)
                        .maxStayDays(180)
                        .entries("MULTIPLE")
                        .allowedPorts(List.of("29 Designated Airports", "5 Major Seaports"))
                        .documents(List.of("Passport photo", "Passport bio page scan"))
                        .sourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(true)
                        .build(),

                // NEPAL (NPL) - TOURISM (VISA FREE)
                VisaRule.builder()
                        .nationality("NPL")
                        .purpose(VisaPurpose.TOURISM)
                        .routeType(RouteType.VISA_FREE)
                        .title("Freedom of Movement (Visa-Free)")
                        .description("Citizens of Nepal do not require a visa to enter India under the 1950 Indo-Nepal Treaty. Sample data — verify with official source.")
                        .fee(BigDecimal.ZERO)
                        .currency("USD")
                        .processingDaysMin(0)
                        .processingDaysMax(0)
                        .validityDays(0)
                        .maxStayDays(0)
                        .entries("FREEDOM_OF_MOVEMENT")
                        .allowedPorts(List.of("All open land borders, airports, and seaports"))
                        .documents(List.of("Valid Nepali Passport or Election Commission Voter ID Card"))
                        .sourceUrl("https://www.mea.gov.in")
                        .lastVerifiedAt(Instant.now())
                        .effectiveFrom(LocalDate.of(2024, 1, 1))
                        .isRecommended(true)
                        .build()
        );

        visaRuleRepository.saveAll(sampleRules);
        log.info("Successfully seeded {} visa rules", sampleRules.size());
    }
}
