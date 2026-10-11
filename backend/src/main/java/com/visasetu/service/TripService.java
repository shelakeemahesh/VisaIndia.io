package com.visasetu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Trip submitTripFromDraft(UUID draftId, Long expectedVersion, String applicantName, String arrivalDateStr, String port) {
        return submitTripFromDraft(draftId, null, expectedVersion, applicantName, arrivalDateStr, port);
    }

    @Transactional
    public Trip submitTripFromDraft(UUID draftId, UUID currentUserId, Long expectedVersion, String applicantName, String arrivalDateStr, String port) {
        return submitTripFromDraft(draftId, currentUserId, null, expectedVersion, applicantName, arrivalDateStr, port);
    }

    @Transactional
    public Trip submitTripFromDraft(UUID draftId, UUID currentUserId, String guestAccessToken, Long expectedVersion, String applicantName, String arrivalDateStr, String port) {
        TripDraft draft = tripDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + draftId));

        // IDOR Prevention: Check ownership
        if (draft.getUserId() != null) {
            if (currentUserId == null || !draft.getUserId().equals(currentUserId)) {
                log.warn("IDOR attempt: User {} attempted to submit draft {} owned by {}", currentUserId, draftId, draft.getUserId());
                throw new AccessDeniedException("You do not have permission to submit this application.");
            }
        } else if (currentUserId == null && draft.getGuestAccessToken() != null) {
            if (guestAccessToken == null || !MessageDigest.isEqual(
                    draft.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    guestAccessToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                log.warn("IDOR attempt: Invalid or missing guest token submitting draft {}", draftId);
                throw new AccessDeniedException("Invalid or missing guest access token.");
            }
        }

        // Prevent duplicate submissions
        if (Boolean.TRUE.equals(draft.getIsSubmitted())) {
            throw new IllegalStateException("Draft has already been submitted as an application (Reference: " + draft.getReferenceId() + ").");
        }

        if (tripRepository.findByReferenceIdIgnoreCase(draft.getReferenceId()).isPresent()) {
            throw new IllegalStateException("Application with reference ID " + draft.getReferenceId() + " already exists in system of record.");
        }

        if (expectedVersion != null && !draft.getVersion().equals(expectedVersion)) {
            throw new OptimisticLockException("Concurrent edit detected prior to submission. Please reload.");
        }

        // Extract draft data
        String rawNationality = draft.getNationality();
        String categoryName = draft.getCategoryName() != null ? draft.getCategoryName() : "e-Tourist Visa (30 Days)";
        String passportPlain = null;
        String dobPlain = null;
        String passportExpiryStr = null;

        if (draft.getSharedProfileJson() != null && !draft.getSharedProfileJson().isBlank()) {
            try {
                JsonNode profileNode = objectMapper.readTree(draft.getSharedProfileJson());
                if (profileNode.has("nationality") && !profileNode.get("nationality").asText().isBlank()) {
                    rawNationality = profileNode.get("nationality").asText();
                }
                if (profileNode.has("categoryName") && !profileNode.get("categoryName").asText().isBlank()) {
                    categoryName = profileNode.get("categoryName").asText();
                }
                if (profileNode.has("passportNumber")) {
                    passportPlain = profileNode.get("passportNumber").asText();
                }
                if (profileNode.has("dob")) {
                    dobPlain = profileNode.get("dob").asText();
                }
                if (profileNode.has("passportExpiry")) {
                    passportExpiryStr = profileNode.get("passportExpiry").asText();
                } else if (profileNode.has("passportExpiryDate")) {
                    passportExpiryStr = profileNode.get("passportExpiryDate").asText();
                }
                if ((applicantName == null || applicantName.isBlank()) && profileNode.has("fullName")) {
                    applicantName = profileNode.get("fullName").asText();
                }
            } catch (Exception e) {
                log.warn("Error parsing sharedProfileJson in draft {}: {}", draft.getReferenceId(), e.getMessage());
            }
        }

        // Server-Side Validation: Applicant Name
        String resolvedName = applicantName != null && !applicantName.isBlank()
                ? applicantName.trim()
                : "Traveler " + draft.getReferenceId();
        if (resolvedName.length() < 2) {
            throw new IllegalArgumentException("Applicant full name must be at least 2 characters.");
        }

        // Server-Side Validation: Passport Number
        if (passportPlain != null && !passportPlain.isBlank() && passportPlain.trim().length() < 5) {
            throw new IllegalArgumentException("Valid passport number is required (minimum 5 characters).");
        }

        // Server-Side Validation: Date of Birth
        if (dobPlain != null && !dobPlain.isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(dobPlain.trim());
                if (!dob.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Date of birth must be in the past.");
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date of birth format: '" + dobPlain + "'. Expected format: YYYY-MM-DD.");
            }
        }

        // Server-Side Validation: Arrival Date
        LocalDate arrivalDate;
        if (arrivalDateStr != null && !arrivalDateStr.isBlank()) {
            try {
                arrivalDate = LocalDate.parse(arrivalDateStr.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid arrival date format: '" + arrivalDateStr + "'. Expected format: YYYY-MM-DD.");
            }
        } else {
            arrivalDate = LocalDate.now().plusDays(20);
        }
        if (!arrivalDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Arrival date must be in the future.");
        }

        // Server-Side Validation: Passport Expiry (at least 6 months / 180 days after arrival)
        if (passportExpiryStr != null && !passportExpiryStr.isBlank()) {
            try {
                LocalDate passportExpiry = LocalDate.parse(passportExpiryStr.trim());
                if (passportExpiry.isBefore(arrivalDate.plusDays(180))) {
                    throw new IllegalArgumentException("Passport must be valid for at least 6 months (180 days) beyond intended arrival date (" + arrivalDate + "). Current expiry: " + passportExpiry);
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid passport expiry date format: '" + passportExpiryStr + "'. Expected format: YYYY-MM-DD.");
            }
        }

        String iso3 = CountryRegistry.resolveIso3Code(rawNationality);

        // Strict Server-Side Eligibility & Fee Resolution matching category and purpose
        VisaPurpose resolvedPurpose = resolveVisaPurpose(categoryName);
        List<VisaRule> rules = visaRuleRepository.findActiveRules(iso3, resolvedPurpose);
        VisaRule matchingRule = null;
        for (VisaRule r : rules) {
            if (r.getTitle().equalsIgnoreCase(categoryName)
                    || categoryName.toLowerCase().contains(r.getTitle().toLowerCase())
                    || r.getTitle().toLowerCase().contains(categoryName.toLowerCase())) {
                matchingRule = r;
                break;
            }
        }
        if (matchingRule == null && !rules.isEmpty()) {
            for (VisaRule r : rules) {
                if (r.getPurpose() == resolvedPurpose) {
                    matchingRule = r;
                    break;
                }
            }
        }

        if (matchingRule == null) {
            log.error("Submission rejected: No active visa rule found for nationality {} (Purpose: {}, Category: {})",
                    iso3, resolvedPurpose, categoryName);
            throw new IllegalArgumentException("No eligible visa rule available for nationality " + iso3 + " and category " + categoryName + ".");
        }

        BigDecimal serverCalculatedFee = matchingRule.getFee();
        String serverCurrency = matchingRule.getCurrency();

        // Encrypt Sensitive PII (Passport number & DOB) with AES-256-GCM
        String passportEncrypted = (passportPlain != null && !passportPlain.isBlank())
                ? encryptionService.encrypt(passportPlain.trim())
                : null;
        String dobEncrypted = (dobPlain != null && !dobPlain.isBlank())
                ? encryptionService.encrypt(dobPlain.trim())
                : null;

        // Atomic snapshot into PostgreSQL
        Trip trip = Trip.builder()
                .referenceId(draft.getReferenceId())
                .userId(draft.getUserId() != null ? draft.getUserId() : currentUserId)
                .guestAccessToken(draft.getGuestAccessToken())
                .preparationStatus("PREPARATION_IN_PROGRESS")
                .applicantEmail(draft.getApplicantEmail())
                .applicantName(resolvedName)
                .passportNumberEncrypted(passportEncrypted)
                .dateOfBirthEncrypted(dobEncrypted)
                .nationality(iso3)
                .categoryName(matchingRule.getTitle())
                .status("SUBMITTED")
                .totalAmount(serverCalculatedFee)
                .currency(serverCurrency)
                .arrivalDate(arrivalDate)
                .portOfArrival(port != null && !port.isBlank() ? port : "Delhi (DEL)")
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .submittedAt(Instant.now())
                .build();

        Trip savedTrip = tripRepository.save(trip);

        // Mark Draft as Submitted to prevent duplicate submissions
        draft.setIsSubmitted(true);
        draft.setTripId(savedTrip.getId());
        draft.setUpdatedAt(Instant.now());
        tripDraftRepository.save(draft);

        // Extract health declaration consent from module data
        boolean healthDeclarationAccepted = false;
        if (draft.getModulesJson() != null && !draft.getModulesJson().isBlank()) {
            try {
                JsonNode modNode = objectMapper.readTree(draft.getModulesJson());
                if (modNode.has("eSuvidha") && modNode.get("eSuvidha").has("healthDeclarationAccepted")) {
                    healthDeclarationAccepted = modNode.get("eSuvidha").get("healthDeclarationAccepted").asBoolean();
                } else if (modNode.has("healthDeclarationAccepted")) {
                    healthDeclarationAccepted = modNode.get("healthDeclarationAccepted").asBoolean();
                }
            } catch (Exception ignored) {}
        }

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
                .status(healthDeclarationAccepted ? "VALIDATED" : "PENDING_CONSENT")
                .formDataJson("{\"healthDeclarationAccepted\":" + healthDeclarationAccepted + "}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        tripModuleRepository.saveAll(List.of(eVisaModule, eArrivalModule, eSuvidhaModule));
        log.info("Successfully took atomic snapshot of draft {} into Trip {} with calculated fee {} {}",
                draft.getReferenceId(), savedTrip.getId(), serverCalculatedFee, serverCurrency);

        return savedTrip;
    }

    private VisaPurpose resolveVisaPurpose(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return VisaPurpose.TOURISM;
        }
        String lower = categoryName.toLowerCase();
        if (lower.contains("business")) {
            return VisaPurpose.BUSINESS;
        } else if (lower.contains("medical")) {
            return VisaPurpose.MEDICAL;
        } else if (lower.contains("conference")) {
            return VisaPurpose.CONFERENCE;
        }
        return VisaPurpose.TOURISM;
    }

    @Transactional(readOnly = true)
    public Optional<Trip> getTripByReference(String referenceId) {
        return tripRepository.findByReferenceIdIgnoreCase(referenceId);
    }

    @Transactional(readOnly = true)
    public List<Trip> getUserTrips(UUID userId) {
        if (userId == null) return List.of();
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
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
