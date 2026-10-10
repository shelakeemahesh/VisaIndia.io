package com.visasetu.service;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.domain.entity.TripModule;
import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.VisaPurpose;
import com.visasetu.repository.TripDraftRepository;
import com.visasetu.repository.TripModuleRepository;
import com.visasetu.repository.TripRepository;
import com.visasetu.repository.VisaRuleRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripModuleRepository tripModuleRepository;
    private final TripDraftRepository tripDraftRepository;
    private final VisaRuleRepository visaRuleRepository;

    @Transactional
    public Trip submitTripFromDraft(UUID draftId, Long expectedVersion, String applicantName, String arrivalDateStr, String port) {
        TripDraft draft = tripDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        if (expectedVersion != null && !draft.getVersion().equals(expectedVersion)) {
            throw new OptimisticLockException("Concurrent edit detected prior to submission. Please reload.");
        }

        // Determine fee based on nationality and rules
        String nationality = "USA";
        BigDecimal fee = new BigDecimal("25.00");
        String categoryName = "e-Tourist Visa (30 Days)";

        try {
            List<VisaRule> rules = visaRuleRepository.findActiveRules("JPN", VisaPurpose.TOURISM);
            if (!rules.isEmpty()) {
                fee = rules.get(0).getFee();
            }
        } catch (Exception ignored) {}

        LocalDate arrivalDate = arrivalDateStr != null && !arrivalDateStr.isBlank()
                ? LocalDate.parse(arrivalDateStr)
                : LocalDate.now().plusDays(20);

        // Atomic snapshot into PostgreSQL
        Trip trip = Trip.builder()
                .referenceId(draft.getReferenceId())
                .applicantEmail(draft.getApplicantEmail())
                .applicantName(applicantName != null && !applicantName.isBlank() ? applicantName : "Traveler " + draft.getReferenceId())
                .nationality(nationality)
                .categoryName(categoryName)
                .status("SUBMITTED")
                .totalAmount(fee)
                .currency("USD")
                .arrivalDate(arrivalDate)
                .portOfArrival(port != null && !port.isBlank() ? port : "Delhi (DEL)")
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .submittedAt(Instant.now())
                .build();

        Trip savedTrip = tripRepository.save(trip);

        // Persist 3 Pluggable Modules
        TripModule eVisaModule = TripModule.builder()
                .tripId(savedTrip.getId())
                .moduleType("E_VISA")
                .status("VALIDATED")
                .formDataJson(draft.getModulesJson())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TripModule eArrivalModule = TripModule.builder()
                .tripId(savedTrip.getId())
                .moduleType("E_ARRIVAL")
                .status("VALIDATED")
                .formDataJson(draft.getSharedProfileJson())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TripModule eSuvidhaModule = TripModule.builder()
                .tripId(savedTrip.getId())
                .moduleType("E_SUVIDHA")
                .status("VALIDATED")
                .formDataJson("{\"healthDeclarationAccepted\":true}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        tripModuleRepository.saveAll(List.of(eVisaModule, eArrivalModule, eSuvidhaModule));
        log.info("Successfully took atomic snapshot of draft {} into Trip {} with 3 pluggable modules",
                draft.getReferenceId(), savedTrip.getId());

        return savedTrip;
    }

    @Transactional(readOnly = true)
    public Optional<Trip> getTripByReference(String referenceId) {
        return tripRepository.findByReferenceIdIgnoreCase(referenceId);
    }

    @Transactional(readOnly = true)
    public List<TripModule> getTripModules(UUID tripId) {
        return tripModuleRepository.findByTripId(tripId);
    }

    @Transactional
    public Trip updateTripStatus(UUID tripId, String newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        trip.setStatus(newStatus);
        trip.setUpdatedAt(Instant.now());
        return tripRepository.save(trip);
    }
}
