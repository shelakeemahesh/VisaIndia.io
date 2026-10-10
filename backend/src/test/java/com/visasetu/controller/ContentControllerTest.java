package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.service.JwtTokenService;
import com.visasetu.domain.entity.EntryPoint;
import com.visasetu.domain.entity.Notice;
import com.visasetu.domain.entity.VisaCategoryDetail;
import com.visasetu.repository.EntryPointRepository;
import com.visasetu.repository.FaqRepository;
import com.visasetu.repository.NoticeRepository;
import com.visasetu.repository.VisaCategoryDetailRepository;
import com.visasetu.service.ContentIngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtTokenService.class})
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisaCategoryDetailRepository visaCategoryDetailRepository;

    @MockBean
    private EntryPointRepository entryPointRepository;

    @MockBean
    private NoticeRepository noticeRepository;

    @MockBean
    private FaqRepository faqRepository;

    @MockBean
    private ContentIngestionService contentIngestionService;

    @Test
    @DisplayName("GET /api/v1/content/categories returns category list")
    void shouldReturnCategories() throws Exception {
        VisaCategoryDetail detail = VisaCategoryDetail.builder()
                .slug("e-tourist-30-days")
                .name("e-Tourist Visa (30 Days)")
                .subCategoryName("e-Tourist Visa (01 Month)")
                .code("ETV_30D")
                .purposeCode("TOURISM")
                .description("30 Days tourist visa")
                .whoIsItFor("Tourists")
                .validityText("30 Days")
                .validityDays(30)
                .entries("DOUBLE")
                .maxStayText("30 Days")
                .maxStayDays(30)
                .feeBandSummary("$25 USD")
                .processingDaysMin(3)
                .processingDaysMax(4)
                .requiredDocuments(List.of("Photo", "Passport"))
                .officialSourceUrl("https://indianvisaonline.gov.in")
                .build();

        given(visaCategoryDetailRepository.findAll()).willReturn(List.of(detail));

        mockMvc.perform(get("/api/v1/content/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("e-tourist-30-days"))
                .andExpect(jsonPath("$[0].name").value("e-Tourist Visa (30 Days)"));
    }

    @Test
    @DisplayName("GET /api/v1/content/categories/{slug} returns single category")
    void shouldReturnCategoryBySlug() throws Exception {
        VisaCategoryDetail detail = VisaCategoryDetail.builder()
                .slug("e-business-visa")
                .name("e-Business Visa")
                .subCategoryName("e-Business Visa (1 Year)")
                .code("EBV_1Y")
                .purposeCode("BUSINESS")
                .description("Business visa")
                .whoIsItFor("Businessmen")
                .validityText("1 Year")
                .validityDays(365)
                .entries("MULTIPLE")
                .maxStayText("180 Days")
                .maxStayDays(180)
                .feeBandSummary("$80 USD")
                .processingDaysMin(3)
                .processingDaysMax(4)
                .requiredDocuments(List.of("Business Card"))
                .officialSourceUrl("https://indianvisaonline.gov.in")
                .build();

        given(visaCategoryDetailRepository.findBySlug("e-business-visa")).willReturn(Optional.of(detail));

        mockMvc.perform(get("/api/v1/content/categories/e-business-visa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("e-business-visa"))
                .andExpect(jsonPath("$.code").value("EBV_1Y"));
    }

    @Test
    @DisplayName("GET /api/v1/content/entry-points returns designated ports")
    void shouldReturnEntryPoints() throws Exception {
        EntryPoint pt = EntryPoint.builder()
                .name("Indira Gandhi International Airport, Delhi")
                .code("DEL")
                .type("AIRPORT")
                .state("Delhi")
                .isEvisaEntry(true)
                .isEvisaExit(true)
                .isVoaAllowed(true)
                .officialSourceUrl("https://boi.gov.in/")
                .build();

        given(entryPointRepository.findAll()).willReturn(List.of(pt));

        mockMvc.perform(get("/api/v1/content/entry-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DEL"))
                .andExpect(jsonPath("$[0].type").value("AIRPORT"));
    }

    @Test
    @DisplayName("GET /api/v1/content/notices returns published advisories")
    void shouldReturnNotices() throws Exception {
        Notice notice = Notice.builder()
                .noticeId("NOT-2026-08")
                .title("Mandatory Pre-Arrival e-Arrival Card")
                .summary("Advisory summary")
                .body("Advisory body")
                .category("POLICY")
                .severity("CRITICAL")
                .publishedAt(Instant.now())
                .sourceUrl("https://boi.gov.in/")
                .isPinnedHome(true)
                .build();

        given(noticeRepository.findByOrderByPublishedAtDesc()).willReturn(List.of(notice));

        mockMvc.perform(get("/api/v1/content/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noticeId").value("NOT-2026-08"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }
}
