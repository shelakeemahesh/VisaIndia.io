package com.visasetu.controller;

import com.visasetu.domain.entity.HelpTicket;
import com.visasetu.repository.HelpTicketRepository;
import com.visasetu.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
@Slf4j
public class HelpTicketController {

    private final HelpTicketRepository helpTicketRepository;
    private final RateLimitingService rateLimitingService;

    @Data
    public static class TicketRequest {
        private String category;
        @NotBlank
        private String subject;
        @NotBlank
        private String description;
        @NotBlank
        @Email
        private String email;
        private String referenceId;
    }

    @PostMapping("/report-problem")
    public ResponseEntity<?> submitProblemReport(@RequestBody TicketRequest request, HttpServletRequest httpRequest) {
        if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email address is required."));
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Subject is required."));
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description is required."));
        }

        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimitingService.tryAcquire("ticket:" + clientIp, 5, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many problem reports submitted. Please wait before submitting another inquiry."
            ));
        }

        String ticketId = "TKT-" + Instant.now().getEpochSecond() % 1000000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String cat = request.getCategory() != null && !request.getCategory().isBlank() ? request.getCategory().trim().toUpperCase() : "GENERAL";

        HelpTicket ticket = HelpTicket.builder()
                .ticketId(ticketId)
                .category(cat)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .email(request.getEmail().trim().toLowerCase())
                .referenceId(request.getReferenceId() != null ? request.getReferenceId().trim() : null)
                .status("RECEIVED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        helpTicketRepository.save(ticket);
        log.info("Persisted help ticket {} for email {} (category: {})", ticketId, ticket.getEmail(), cat);

        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "RECEIVED",
                "message", "Your inquiry has been logged. Our consular support desk typically responds within 4 to 8 business hours.",
                "receivedAt", Instant.now().toString(),
                "category", cat
        ));
    }
}
