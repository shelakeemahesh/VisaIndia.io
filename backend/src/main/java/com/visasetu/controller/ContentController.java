package com.visasetu.controller;

import com.visasetu.domain.entity.EntryPoint;
import com.visasetu.domain.entity.Faq;
import com.visasetu.domain.entity.Notice;
import com.visasetu.domain.entity.VisaCategoryDetail;
import com.visasetu.repository.EntryPointRepository;
import com.visasetu.repository.FaqRepository;
import com.visasetu.repository.NoticeRepository;
import com.visasetu.repository.VisaCategoryDetailRepository;
import com.visasetu.service.ContentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

    private final VisaCategoryDetailRepository visaCategoryDetailRepository;
    private final EntryPointRepository entryPointRepository;
    private final NoticeRepository noticeRepository;
    private final FaqRepository faqRepository;
    private final ContentIngestionService contentIngestionService;

    @GetMapping("/categories")
    public ResponseEntity<List<VisaCategoryDetail>> getAllCategories(
            @RequestParam(required = false) String purpose
    ) {
        if (purpose != null && !purpose.isBlank()) {
            return ResponseEntity.ok(visaCategoryDetailRepository.findByPurposeCode(purpose.toUpperCase()));
        }
        return ResponseEntity.ok(visaCategoryDetailRepository.findAll());
    }

    @GetMapping("/categories/{slug}")
    public ResponseEntity<VisaCategoryDetail> getCategoryBySlug(@PathVariable String slug) {
        return visaCategoryDetailRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entry-points")
    public ResponseEntity<List<EntryPoint>> getEntryPoints(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String state
    ) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(entryPointRepository.findByTypeOrderByStateAscNameAsc(type.toUpperCase()));
        }
        if (state != null && !state.isBlank()) {
            return ResponseEntity.ok(entryPointRepository.findByStateIgnoreCase(state));
        }
        return ResponseEntity.ok(entryPointRepository.findAll());
    }

    @GetMapping("/notices")
    public ResponseEntity<List<Notice>> getNotices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean pinnedOnly
    ) {
        if (pinnedOnly) {
            return ResponseEntity.ok(noticeRepository.findByIsPinnedHomeTrueOrderByPublishedAtDesc());
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(noticeRepository.findByCategoryOrderByPublishedAtDesc(category.toUpperCase()));
        }
        return ResponseEntity.ok(noticeRepository.findByOrderByPublishedAtDesc());
    }

    @GetMapping("/notices/{id}")
    public ResponseEntity<Notice> getNoticeById(@PathVariable String id) {
        return noticeRepository.findByNoticeId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/faqs")
    public ResponseEntity<List<Faq>> getFaqs(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String query
    ) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(faqRepository.searchFaqs(query.trim()));
        }
        if (topic != null && !topic.isBlank()) {
            return ResponseEntity.ok(faqRepository.findByTopicOrderByOrderNumAsc(topic.toUpperCase()));
        }
        return ResponseEntity.ok(faqRepository.findAllByOrderByOrderNumAsc());
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerIngestion() {
        log.info("Triggering on-demand content ingestion pipeline");
        Map<String, Object> result = contentIngestionService.runNightlyIngestion();
        return ResponseEntity.ok(result);
    }
}
