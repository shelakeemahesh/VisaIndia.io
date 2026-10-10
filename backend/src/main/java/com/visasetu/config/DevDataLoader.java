package com.visasetu.config;

import com.visasetu.domain.entity.*;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader {

    private final VisaRuleRepository visaRuleRepository;
    private final VisaCategoryDetailRepository visaCategoryDetailRepository;
    private final EntryPointRepository entryPointRepository;
    private final NoticeRepository noticeRepository;
    private final FaqRepository faqRepository;
    private final DemoApplicationRepository demoApplicationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDataIfEmpty() {
        seedVisaRules();
        seedVisaCategories();
        seedEntryPoints();
        seedNotices();
        seedFaqs();
        seedDemoApplications();
    }

    private void seedVisaRules() {
        if (visaRuleRepository.count() > 0) return;

        log.info("Seeding initial visa rules into database...");
        List<VisaRule> sampleRules = List.of(
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

    private void seedVisaCategories() {
        if (visaCategoryDetailRepository.count() > 0) return;

        log.info("Seeding official visa categories into database...");
        List<VisaCategoryDetail> categories = List.of(
                VisaCategoryDetail.builder()
                        .slug("e-tourist-30-days")
                        .name("e-Tourist Visa (30 Days)")
                        .subCategoryName("e-Tourist Visa (01 Month)")
                        .code("ETV_30D")
                        .purposeCode("TOURISM")
                        .description("Short-duration electronic tourist visa for holidaying, sightseeing, and casual visits.")
                        .whoIsItFor("International travelers visiting India for recreation, sightseeing, yoga programs, or visiting family and friends for up to 30 days.")
                        .validityText("30 days from date of first arrival in India")
                        .validityDays(30)
                        .entries("DOUBLE")
                        .maxStayText("30 continuous days per entry")
                        .maxStayDays(30)
                        .feeBandSummary("$10 USD (April to June) / $25 USD (July to March)")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(false)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Recent passport-size color photograph (350x350 to 1000x1000 px, 10KB-1MB, white background)",
                                "Passport biographical page scan (PDF or JPEG, 10KB-300KB) containing min 6 months validity",
                                "Confirmed onward/return travel ticket",
                                "Proof of sufficient financial funds"
                        ))
                        .entryPointLimits("Entry permitted at 37 designated airports and 5 seaports. Exit permitted at all authorized ICPs.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-tourist-1-year")
                        .name("e-Tourist Visa (1 Year)")
                        .subCategoryName("e-Tourist Visa (01 Year)")
                        .code("ETV_1Y")
                        .purposeCode("TOURISM")
                        .description("Multiple-entry electronic tourist authorization valid for one full calendar year from the date of grant.")
                        .whoIsItFor("Frequent leisure travelers and diaspora visiting relatives across multiple trips throughout the year.")
                        .validityText("365 days from the date of Electronic Travel Authorization (ETA) issuance")
                        .validityDays(365)
                        .entries("MULTIPLE")
                        .maxStayText("Continuous stay during each visit shall not exceed 90 days (180 days for USA, UK, Japan, Canada)")
                        .maxStayDays(90)
                        .feeBandSummary("$40 USD for most nationalities (reciprocal variations apply)")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(false)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Recent color passport photograph (white background)",
                                "Clear copy of passport biographical information page",
                                "Sufficient financial means"
                        ))
                        .entryPointLimits("Entry at 37 designated international airports and 5 designated seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-tourist-5-years")
                        .name("e-Tourist Visa (5 Years)")
                        .subCategoryName("e-Tourist Visa (05 Years)")
                        .code("ETV_5Y")
                        .purposeCode("TOURISM")
                        .description("Long-term multiple-entry electronic travel authorization valid for five years.")
                        .whoIsItFor("Long-term international tourists and regular holidaymakers visiting India periodically over a 5-year span.")
                        .validityText("5 calendar years from the date of ETA grant")
                        .validityDays(1825)
                        .entries("MULTIPLE")
                        .maxStayText("Max 90 continuous days per visit (180 days for nationals of USA, UK, Japan, and Canada)")
                        .maxStayDays(90)
                        .feeBandSummary("$80 USD for most eligible nationalities")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(false)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Recent color passport photo (white background)",
                                "Color scan of original passport bio page",
                                "Valid return or onward ticket"
                        ))
                        .entryPointLimits("Entry permitted at 37 designated airports and 5 designated seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-business-visa")
                        .name("e-Business Visa")
                        .subCategoryName("e-Business Visa (1 Year)")
                        .code("EBV_1Y")
                        .purposeCode("BUSINESS")
                        .description("Multiple-entry electronic commercial visa covering corporate meetings, industrial ventures, and trade exhibitions.")
                        .whoIsItFor("Business professionals attending commercial meetings, trade fairs, setting up ventures, recruiting staff, or delivering lectures under GIAN.")
                        .validityText("365 days from the date of ETA grant")
                        .validityDays(365)
                        .entries("MULTIPLE")
                        .maxStayText("Continuous stay up to 180 days per calendar year. (FRRO registration required if cumulative stay exceeds 180 days)")
                        .maxStayDays(180)
                        .feeBandSummary("$80 USD to $100 USD (UK $100 USD, USA $100 USD)")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(false)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Recent passport-size color photograph",
                                "Color copy of passport bio page",
                                "Visiting card / Business card of applicant",
                                "Invitation letter from Indian organization/firm with company letterhead and CIN"
                        ))
                        .entryPointLimits("Entry permitted at 37 designated airports and 5 seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-medical-visa")
                        .name("e-Medical Visa")
                        .subCategoryName("e-Medical Visa (60 Days)")
                        .code("EMV_60D")
                        .purposeCode("MEDICAL")
                        .description("Triple-entry electronic visa granted for receiving specialized medical treatment in recognized Indian hospitals.")
                        .whoIsItFor("Patients seeking medical treatment, advanced surgical procedures, or organ transplants at accredited super-specialty hospitals in India.")
                        .validityText("60 days from the date of first arrival in India")
                        .validityDays(60)
                        .entries("TRIPLE")
                        .maxStayText("Up to 60 days per trip with triple entry permissions within validity window")
                        .maxStayDays(60)
                        .feeBandSummary("$80 USD for most nationalities")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(true)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Recent color photograph",
                                "Passport biographical page scan",
                                "Official letter from accredited hospital in India stating medical diagnosis and treatment schedule on hospital letterhead"
                        ))
                        .entryPointLimits("Entry permitted at 37 designated airports and 5 seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-medical-attendant")
                        .name("e-Medical Attendant Visa")
                        .subCategoryName("e-Medical Attendant Visa")
                        .code("EMAV_60D")
                        .purposeCode("MEDICAL")
                        .description("Electronic visa issued to family members or caregivers accompanying an e-Medical visa holder.")
                        .whoIsItFor("Up to two attendants/caregivers traveling together with the principal e-Medical patient.")
                        .validityText("Co-terminus with the principal e-Medical Visa holder (60 days from arrival)")
                        .validityDays(60)
                        .entries("TRIPLE")
                        .maxStayText("Matches the principal patient’s authorized duration of stay")
                        .maxStayDays(60)
                        .feeBandSummary("$80 USD per attendant")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(true)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Color photo and passport bio page",
                                "Copy of the principal e-Medical patient’s application reference or ETA approval letter"
                        ))
                        .entryPointLimits("Must travel with or immediately follow the principal medical patient.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-conference-visa")
                        .name("e-Conference Visa")
                        .subCategoryName("e-Conference Visa (30 Days)")
                        .code("ECV_30D")
                        .purposeCode("CONFERENCE")
                        .description("Single-entry electronic visa granted to international delegates attending seminars and government-cleared conferences.")
                        .whoIsItFor("Academicians, scientists, industry delegates, and speakers invited to official workshops and international conferences held in India.")
                        .validityText("30 days from date of arrival in India")
                        .validityDays(30)
                        .entries("SINGLE")
                        .maxStayText("30 continuous days")
                        .maxStayDays(30)
                        .feeBandSummary("$80 USD for most nationalities")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(false)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Passport photo and bio page",
                                "Formal invitation letter from conference organizer in India",
                                "Political Clearance from Ministry of External Affairs (MEA)",
                                "Event Security Clearance from Ministry of Home Affairs (MHA)"
                        ))
                        .entryPointLimits("Entry permitted at 37 designated airports and 5 seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                VisaCategoryDetail.builder()
                        .slug("e-ayush-visa")
                        .name("e-Ayush Visa")
                        .subCategoryName("e-Ayush Visa (Traditional Medicine)")
                        .code("EAV_1Y")
                        .purposeCode("MEDICAL")
                        .description("Specialized electronic visa for receiving therapeutic care in Ayurveda, Yoga, Naturopathy, Unani, Siddha, and Homeopathy.")
                        .whoIsItFor("International patients seeking holistic treatments at accredited Ayush hospitals and wellness institutions in India.")
                        .validityText("Up to 1 year with multiple entries as per medical recommendation")
                        .validityDays(365)
                        .entries("MULTIPLE")
                        .maxStayText("Up to 180 days per visit")
                        .maxStayDays(180)
                        .feeBandSummary("$80 USD")
                        .processingDaysMin(3)
                        .processingDaysMax(4)
                        .isExtendable(true)
                        .isConvertible(false)
                        .requiredDocuments(List.of(
                                "Passport bio page and photo",
                                "Letter from NABH/accredited Ayush wellness center or hospital in India specifying treatment program"
                        ))
                        .entryPointLimits("Entry at 37 designated airports and 5 designated seaports.")
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build()
        );

        visaCategoryDetailRepository.saveAll(categories);
        log.info("Successfully seeded {} visa category details", categories.size());
    }

    private void seedEntryPoints() {
        if (entryPointRepository.count() > 0) return;

        log.info("Seeding designated entry points into database...");
        List<EntryPoint> points = List.of(
                EntryPoint.builder().name("Indira Gandhi International Airport, Delhi").code("DEL").type("AIRPORT").state("Delhi").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Chhatrapati Shivaji Maharaj International Airport, Mumbai").code("BOM").type("AIRPORT").state("Maharashtra").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Kempegowda International Airport, Bengaluru").code("BLR").type("AIRPORT").state("Karnataka").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Chennai International Airport, Chennai").code("MAA").type("AIRPORT").state("Tamil Nadu").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Netaji Subhash Chandra Bose International Airport, Kolkata").code("CCU").type("AIRPORT").state("West Bengal").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Rajiv Gandhi International Airport, Hyderabad").code("HYD").type("AIRPORT").state("Telangana").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(true).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Sardar Vallabhbhai Patel International Airport, Ahmedabad").code("AMD").type("AIRPORT").state("Gujarat").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Cochin International Airport, Kochi").code("COK").type("AIRPORT").state("Kerala").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Dabolim Airport, Goa").code("GOI").type("AIRPORT").state("Goa").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Manohar International Airport, Mopa, Goa").code("GOX").type("AIRPORT").state("Goa").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Sri Guru Ram Dass Jee International Airport, Amritsar").code("ATQ").type("AIRPORT").state("Punjab").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Jaipur International Airport, Jaipur").code("JAI").type("AIRPORT").state("Rajasthan").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Chaudhary Charan Singh International Airport, Lucknow").code("LKO").type("AIRPORT").state("Uttar Pradesh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Trivandrum International Airport, Thiruvananthapuram").code("TRV").type("AIRPORT").state("Kerala").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Lal Bahadur Shastri Airport, Varanasi").code("VNS").type("AIRPORT").state("Uttar Pradesh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Coimbatore International Airport, Coimbatore").code("CJB").type("AIRPORT").state("Tamil Nadu").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Lokpriya Gopinath Bordoloi International Airport, Guwahati").code("GAU").type("AIRPORT").state("Assam").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Mangaluru International Airport, Mangalore").code("IXE").type("AIRPORT").state("Karnataka").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Dr. Babasaheb Ambedkar International Airport, Nagpur").code("NAG").type("AIRPORT").state("Maharashtra").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Pune International Airport, Pune").code("PNQ").type("AIRPORT").state("Maharashtra").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Tiruchirappalli International Airport, Trichy").code("TRZ").type("AIRPORT").state("Tamil Nadu").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Visakhapatnam Airport, Visakhapatnam").code("VTZ").type("AIRPORT").state("Andhra Pradesh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Chandigarh International Airport, Chandigarh").code("IXC").type("AIRPORT").state("Chandigarh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Madurai Airport, Madurai").code("IXM").type("AIRPORT").state("Tamil Nadu").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Biju Patnaik International Airport, Bhubaneswar").code("BBI").type("AIRPORT").state("Odisha").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Veer Savarkar International Airport, Port Blair").code("IXZ").type("AIRPORT").state("Andaman and Nicobar").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Bagdogra Airport, Siliguri").code("IXB").type("AIRPORT").state("West Bengal").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Calicut International Airport, Kozhikode").code("CCJ").type("AIRPORT").state("Kerala").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Gaya Airport, Gaya").code("GAY").type("AIRPORT").state("Bihar").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Devi Ahilyabai Holkar Airport, Indore").code("IDR").type("AIRPORT").state("Madhya Pradesh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Kannur International Airport, Kannur").code("CNN").type("AIRPORT").state("Kerala").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Surat International Airport, Surat").code("STV").type("AIRPORT").state("Gujarat").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Vijayawada International Airport, Vijayawada").code("VGA").type("AIRPORT").state("Andhra Pradesh").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Sheikh ul-Alam International Airport, Srinagar").code("SXR").type("AIRPORT").state("Jammu and Kashmir").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),

                // Seaports
                EntryPoint.builder().name("Cochin Seaport, Kochi").code("INCOK").type("SEAPORT").state("Kerala").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Mormugao Seaport, Goa").code("INMRM").type("SEAPORT").state("Goa").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("New Mangalore Seaport, Mangaluru").code("INNML").type("SEAPORT").state("Karnataka").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Mumbai Seaport, Mumbai").code("INBOM").type("SEAPORT").state("Maharashtra").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Chennai Seaport, Chennai").code("INMAA").type("SEAPORT").state("Tamil Nadu").isEvisaEntry(true).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),

                // Land Ports (Permitted for Exit with e-Visa)
                EntryPoint.builder().name("Attari Integrated Check Post, Wagah Border").code("ATTARI").type("LAND_ICP").state("Punjab").isEvisaEntry(false).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Haridaspur / Petrapole Integrated Check Post").code("PETRAPOLE").type("LAND_ICP").state("West Bengal").isEvisaEntry(false).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build(),
                EntryPoint.builder().name("Raxaul Integrated Check Post").code("RAXAUL").type("LAND_ICP").state("Bihar").isEvisaEntry(false).isEvisaExit(true).isVoaAllowed(false).officialSourceUrl("https://boi.gov.in/").build()
        );

        entryPointRepository.saveAll(points);
        log.info("Successfully seeded {} entry points", points.size());
    }

    private void seedNotices() {
        if (noticeRepository.count() > 0) return;

        log.info("Seeding official notices into database...");
        List<Notice> notices = List.of(
                Notice.builder()
                        .noticeId("NOT-2026-08")
                        .title("Mandatory Pre-Arrival e-Arrival Card for All Incoming Foreign Nationals")
                        .summary("All international travelers and OCI cardholders must complete the digital e-Arrival registration before boarding flights to India.")
                        .body("In accordance with Bureau of Immigration guidelines, the paper disembarkation card has been phased out. Incoming travelers must file their personal, flight, and baggage declaration electronically through the unified travel portal prior to embarkation. Verification QR is scanned at designated biometric e-gates upon landing.")
                        .category("POLICY")
                        .severity("CRITICAL")
                        .publishedAt(Instant.now().minusSeconds(86400 * 5))
                        .sourceUrl("https://boi.gov.in/")
                        .isPinnedHome(true)
                        .build(),

                Notice.builder()
                        .noticeId("NOT-2026-05")
                        .title("Visa on Arrival (VoA) Facility Active for Nationals of Japan, South Korea and UAE")
                        .summary("Eligible citizens can obtain double-entry VoA directly at 6 major international airports in India without prior electronic application.")
                        .body("Citizens of Japan, Republic of Korea, and the United Arab Emirates (subject to conditions) may avail the Visa-on-Arrival facility for tourism, business, and medical visits up to 60 days. The facility is operational 24x7 at Delhi, Mumbai, Kolkata, Chennai, Bengaluru, and Hyderabad international airports for a fee of 2000 INR / $25 USD.")
                        .category("ENTRY_POINTS")
                        .severity("HIGH")
                        .publishedAt(Instant.now().minusSeconds(86400 * 15))
                        .sourceUrl("https://indianvisaonline.gov.in/visa/visa-on-arrival.html")
                        .isPinnedHome(false)
                        .build(),

                Notice.builder()
                        .noticeId("NOT-2026-02")
                        .title("Public Fraud Warning: Clone & Intermediary Scam Portals")
                        .summary("Government of India cautions travelers that official e-Visa applications are processed strictly via official government endpoints.")
                        .body("Multiple third-party clone websites impersonating the official Indian e-Visa portal charge exorbitant fees or compromise passport data. Travelers are advised that the authentic government domain is indianvisaonline.gov.in. VisaSetu operates exclusively as a concept demonstration and does not collect live fees on behalf of the Government.")
                        .category("SYSTEM")
                        .severity("NORMAL")
                        .publishedAt(Instant.now().minusSeconds(86400 * 30))
                        .sourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .isPinnedHome(false)
                        .build(),

                Notice.builder()
                        .noticeId("NOT-2026-01")
                        .title("Yellow Fever & Oral Polio Health Declaration Advisory")
                        .summary("Mandatory Yellow Fever vaccination card required for passengers arriving from or transiting through endemic countries.")
                        .body("Any traveler arriving in India within 6 days of departure from an endemic Yellow Fever zone without a valid international vaccination certificate is liable to quarantine for up to 6 days. Health declarations must be filed in advance under e-Suvidha.")
                        .category("POLICY")
                        .severity("NORMAL")
                        .publishedAt(Instant.now().minusSeconds(86400 * 60))
                        .sourceUrl("https://www.mohfw.gov.in/")
                        .isPinnedHome(false)
                        .build()
        );

        noticeRepository.saveAll(notices);
        log.info("Successfully seeded {} official notices", notices.size());
    }

    private void seedFaqs() {
        if (faqRepository.count() > 0) return;

        log.info("Seeding official FAQs into database...");
        List<Faq> faqs = List.of(
                Faq.builder()
                        .topic("BEFORE_APPLYING")
                        .question("Who is eligible to apply for an Indian e-Visa?")
                        .answer("Nationals of 172+ eligible countries whose sole objective of visiting India is recreation, sightseeing, casual visit to meet friends or relatives, short duration medical treatment, traditional Ayush therapy, commercial business, or attending an authorized international conference. Passports must have at least 6 months validity from the date of arrival and at least 2 blank pages.")
                        .orderNum(1)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("BEFORE_APPLYING")
                        .question("How early can I submit an e-Visa application?")
                        .answer("For the 30-day e-Tourist Visa, applications can be submitted between 30 days and 4 days prior to your arrival date. For 1-Year and 5-Year e-Tourist, e-Business, and e-Medical visas, applications can be submitted up to 120 days in advance of your expected travel date.")
                        .orderNum(2)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("APPLICATION")
                        .question("What are the exact specifications for the passport photograph?")
                        .answer("The photograph must be recent, in color, in JPEG format between 10 KB and 1 MB. Dimensions must be equal (square, 1:1 aspect ratio) with minimum 350x350 pixels up to 1000x1000 pixels. The background must be pure white or light, with your full face centered covering 70% to 80% of the frame with no eyeglasses or shadows.")
                        .orderNum(3)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("APPLICATION")
                        .question("Can I change my port of arrival after my e-Visa is approved?")
                        .answer("Yes. You may arrive at ANY of the 37 designated international airports or 5 designated seaports regardless of the port of arrival indicated on your application. You can exit from any authorized Indian Immigration Check Post (ICP).")
                        .orderNum(4)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("PAYMENT")
                        .question("What should I do if my bank account was debited but the application status still shows Pending?")
                        .answer("VisaSetu includes an automatic payment reconciliation engine that queries the payment gateway every 5 minutes. You can also use the 'Payment debited but not showing?' button on the Existing Applications page to trigger an instantaneous re-sync against the gateway.")
                        .orderNum(5)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("PAYMENT")
                        .question("How many payment attempts am I allowed before the application locks?")
                        .answer("A maximum of 3 payment attempts are permitted per application. If all 3 attempts fail, you may regenerate a fresh checkout session with a new idempotency key on VisaSetu without having to re-enter your applicant profile.")
                        .orderNum(6)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("AFTER_APPROVAL")
                        .question("What document do I receive once my application is approved?")
                        .answer("You will receive an Electronic Travel Authorization (ETA) grant letter via email and in your VisaSetu dashboard. You must print a physical copy of the ETA and carry it with you for travel and presentation at the airline check-in desk and Indian immigration.")
                        .orderNum(7)
                        .officialSourceUrl("https://indianvisaonline.gov.in/evisa/tvoa.html")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("ARRIVAL")
                        .question("What is the procedure upon landing at the airport in India?")
                        .answer("Upon arrival, follow signs to the dedicated 'e-Visa Counters'. Hand over your passport, printed ETA letter, and e-Arrival QR pass. The immigration officer will capture your biometric fingerprints and facial photo, and stamp a physical visa endorsement in your passport.")
                        .orderNum(8)
                        .officialSourceUrl("https://boi.gov.in/")
                        .lastVerifiedAt(Instant.now())
                        .build(),

                Faq.builder()
                        .topic("TECHNICAL")
                        .question("Can an e-Visa be extended or converted to another visa type while in India?")
                        .answer("e-Visas are strictly non-extendable and non-convertible to any other visa type (such as Employment or Student Visa), except in rare emergencies requiring prolonged hospitalization certified by an accredited state medical board via the FRRO.")
                        .orderNum(9)
                        .officialSourceUrl("https://boi.gov.in/")
                        .lastVerifiedAt(Instant.now())
                        .build()
        );

        faqRepository.saveAll(faqs);
        log.info("Successfully seeded {} official FAQs", faqs.size());
    }

    private void seedDemoApplications() {
        if (demoApplicationRepository.count() > 0) return;

        log.info("Seeding demo applications into database...");
        List<DemoApplication> apps = List.of(
                DemoApplication.builder()
                        .referenceId("VS-2026-8821")
                        .applicantEmail("traveler.draft@example.com")
                        .applicantName("Liam Henderson")
                        .nationality("GBR")
                        .categoryName("e-Tourist Visa (1 Year)")
                        .status("DRAFT")
                        .amount(new BigDecimal("40.00"))
                        .arrivalDate(LocalDate.now().plusDays(40))
                        .portOfArrival("Delhi (DEL)")
                        .createdAt(Instant.now().minusSeconds(3600))
                        .updatedAt(Instant.now().minusSeconds(3600))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-7492")
                        .applicantEmail("elena.rodriguez@example.com")
                        .applicantName("Elena Rodriguez")
                        .nationality("ESP")
                        .categoryName("e-Tourist Visa (30 Days)")
                        .status("SUBMITTED")
                        .orderId("order_demo_7492_sub")
                        .amount(new BigDecimal("25.00"))
                        .arrivalDate(LocalDate.now().plusDays(35))
                        .portOfArrival("Mumbai (BOM)")
                        .createdAt(Instant.now().minusSeconds(10800))
                        .updatedAt(Instant.now().minusSeconds(10800))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-6134")
                        .applicantEmail("david.kim@example.com")
                        .applicantName("David Min-Soo Kim")
                        .nationality("KOR")
                        .categoryName("e-Business Visa")
                        .status("PAYMENT_PENDING")
                        .orderId("order_demo_6134_pend")
                        .amount(new BigDecimal("80.00"))
                        .arrivalDate(LocalDate.now().plusDays(50))
                        .portOfArrival("Bengaluru (BLR)")
                        .createdAt(Instant.now().minusSeconds(900))
                        .updatedAt(Instant.now().minusSeconds(900))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-5509")
                        .applicantEmail("akiko.tanaka@example.com")
                        .applicantName("Akiko Tanaka")
                        .nationality("JPN")
                        .categoryName("e-Tourist Visa (30 Days)")
                        .status("UNDER_REVIEW")
                        .orderId("order_demo_5509_rev")
                        .paymentId("pay_demo_5509_capture")
                        .amount(new BigDecimal("25.00"))
                        .arrivalDate(LocalDate.now().plusDays(25))
                        .portOfArrival("Delhi (DEL)")
                        .createdAt(Instant.now().minusSeconds(86400))
                        .updatedAt(Instant.now().minusSeconds(86400))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-4011")
                        .applicantEmail("sarah.jenkins@example.com")
                        .applicantName("Sarah Jane Jenkins")
                        .nationality("USA")
                        .categoryName("e-Tourist Visa (5 Years)")
                        .status("APPROVED")
                        .orderId("order_demo_4011_app")
                        .paymentId("pay_demo_4011_capture")
                        .amount(new BigDecimal("80.00"))
                        .arrivalDate(LocalDate.now().plusDays(14))
                        .portOfArrival("Delhi (DEL)")
                        .qrData("ETA:VS-2026-4011|NAME:SARAH JANE JENKINS|NAT:USA|CAT:ETOURIST_5Y|VALID_UNTIL:2031-10-25")
                        .createdAt(Instant.now().minusSeconds(86400 * 2))
                        .updatedAt(Instant.now().minusSeconds(86400 * 2))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-3199")
                        .applicantEmail("marcus.dupont@example.com")
                        .applicantName("Marcus Dupont")
                        .nationality("FRA")
                        .categoryName("e-Conference Visa")
                        .status("REJECTED")
                        .orderId("order_demo_3199_rej")
                        .paymentId("pay_demo_3199_capture")
                        .amount(new BigDecimal("80.00"))
                        .arrivalDate(LocalDate.now().plusDays(19))
                        .portOfArrival("Hyderabad (HYD)")
                        .createdAt(Instant.now().minusSeconds(86400 * 4))
                        .updatedAt(Instant.now().minusSeconds(86400 * 4))
                        .build(),

                DemoApplication.builder()
                        .referenceId("VS-2026-2048")
                        .applicantEmail("claudia.muller@example.com")
                        .applicantName("Claudia Müller")
                        .nationality("DEU")
                        .categoryName("e-Tourist Visa (1 Year)")
                        .status("PAYMENT_FAILED")
                        .orderId("order_demo_2048_fail")
                        .amount(new BigDecimal("40.00"))
                        .arrivalDate(LocalDate.now().plusDays(30))
                        .portOfArrival("Cochin (COK)")
                        .createdAt(Instant.now().minusSeconds(21600))
                        .updatedAt(Instant.now().minusSeconds(21600))
                        .build()
        );

        demoApplicationRepository.saveAll(apps);
        log.info("Successfully seeded {} demo applications", apps.size());
    }
}
