package com.visasetu.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/help")
@Slf4j
public class HelpTicketController {

    @Data
    public static class TicketRequest {
        private String category;
        private String subject;
        private String description;
        private String email;
        private String referenceId;
    }

    @PostMapping("/report-problem")
    public ResponseEntity<Map<String, Object>> submitProblemReport(@RequestBody TicketRequest request) {
        String ticketId = "TKT-" + Instant.now().getEpochSecond() % 1000000 + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        log.info("Received help ticket {} for email {} (category: {})", ticketId, request.getEmail(), request.getCategory());

        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "RECEIVED",
                "message", "Your inquiry has been logged. Our consular support desk typically responds within 4 to 8 business hours.",
                "receivedAt", Instant.now().toString(),
                "category", request.getCategory() != null ? request.getCategory() : "GENERAL"
        ));
    }
}
