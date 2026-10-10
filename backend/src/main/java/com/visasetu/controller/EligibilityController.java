package com.visasetu.controller;

import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.dto.EligibilityResponse;
import com.visasetu.dto.NationalityOptionDto;
import com.visasetu.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/eligibility")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EligibilityController {

    private final EligibilityService eligibilityService;

    @GetMapping
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @RequestParam String nationality,
            @RequestParam VisaPurpose purpose,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate) {

        EligibilityResponse response = eligibilityService.checkEligibility(nationality, purpose, arrivalDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nationalities")
    public ResponseEntity<List<NationalityOptionDto>> getNationalities() {
        return ResponseEntity.ok(eligibilityService.getSupportedNationalities());
    }

    @GetMapping("/purposes")
    public ResponseEntity<List<Map<String, String>>> getPurposes() {
        List<Map<String, String>> list = eligibilityService.getSupportedPurposes().stream()
                .map(p -> Map.of(
                        "code", p.name(),
                        "displayName", p.getDisplayName()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }
}
