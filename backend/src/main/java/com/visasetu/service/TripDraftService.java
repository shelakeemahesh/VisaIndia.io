package com.visasetu.service;

import com.visasetu.domain.entity.TripDraft;
import com.visasetu.repository.TripDraftRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripDraftService {

    private final TripDraftRepository tripDraftRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TripDraft createOrGetDraft(String email, String nationality, String categoryName) {
        return createOrGetDraft(null, email, nationality, categoryName);
    }

    @Transactional
    public TripDraft createOrGetDraft(UUID userId, String email, String nationality, String categoryName) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        // Check if user has an existing unsubmitted draft
        if (userId != null) {
            Optional<TripDraft> userDraft = tripDraftRepository.findByUserIdAndIsSubmittedFalse(userId);
            if (userDraft.isPresent()) {
                return userDraft.get();
            }
        }

        if (!normalizedEmail.isBlank()) {
            Optional<TripDraft> emailDraft = tripDraftRepository.findByApplicantEmailIgnoreCaseAndIsSubmittedFalse(normalizedEmail);
            if (emailDraft.isEmpty()) {
                emailDraft = tripDraftRepository.findByApplicantEmailIgnoreCase(normalizedEmail)
                        .filter(d -> !Boolean.TRUE.equals(d.getIsSubmitted()));
            }
            if (emailDraft.isPresent()) {
                TripDraft existing = emailDraft.get();
                if (existing.getUserId() == null && userId != null) {
                    existing.setUserId(userId);
                    return tripDraftRepository.save(existing);
                }
                return existing;
            }
        }

        String refId = "VS-" + Year.now().getValue() + "-" + (1000 + secureRandom.nextInt(9000));
        String resolvedNationality = (nationality != null && !nationality.isBlank()) ? nationality.trim() : "USA";
        String resolvedCategory = (categoryName != null && !categoryName.isBlank()) ? categoryName.trim() : "e-Tourist Visa (30 Days)";

        String initialProfile = "{\"nationality\":\"" + resolvedNationality + "\",\"categoryName\":\"" + resolvedCategory + "\"}";
        String initialModules = "{\"eVisa\":{\"status\":\"DRAFT\"},\"eArrival\":{\"status\":\"DRAFT\"},\"eSuvidha\":{\"status\":\"DRAFT\"}}";

        TripDraft draft = TripDraft.builder()
                .referenceId(refId)
                .userId(userId)
                .applicantEmail(normalizedEmail)
                .nationality(resolvedNationality)
                .categoryName(resolvedCategory)
                .currentStep(1)
                .sharedProfileJson(initialProfile)
                .modulesJson(initialModules)
                .isSubmitted(false)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TripDraft saved = tripDraftRepository.save(draft);
        log.info("Initialized new TripDraft {} for user {} (Email: {})", refId, userId, normalizedEmail);
        return saved;
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        return autosaveDraft(draftId, null, expectedVersion, sharedProfileJson, modulesJson, step);
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, UUID currentUserId, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        TripDraft draft = tripDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found for ID: " + draftId));

        // IDOR Prevention: Check ownership
        if (draft.getUserId() != null && currentUserId != null && !draft.getUserId().equals(currentUserId)) {
            log.warn("IDOR attempt: User {} attempted to modify draft {} owned by {}", currentUserId, draftId, draft.getUserId());
            throw new AccessDeniedException("You do not have permission to modify this draft.");
        }

        if (Boolean.TRUE.equals(draft.getIsSubmitted())) {
            throw new IllegalStateException("Cannot modify an already submitted draft.");
        }

        if (expectedVersion != null && !draft.getVersion().equals(expectedVersion)) {
            log.warn("Optimistic locking conflict on draft {}: client version {}, db version {}",
                    draft.getReferenceId(), expectedVersion, draft.getVersion());
            throw new OptimisticLockException("Concurrent edit detected: Server has version " + draft.getVersion() +
                    ", but request provided version " + expectedVersion + ". Please re-synchronize draft.");
        }

        if (sharedProfileJson != null) draft.setSharedProfileJson(sharedProfileJson);
        if (modulesJson != null) draft.setModulesJson(modulesJson);
        if (step != null) draft.setCurrentStep(step);
        draft.setUpdatedAt(Instant.now());

        TripDraft saved = tripDraftRepository.save(draft);
        log.debug("Autosaved draft {} (New Version: {})", saved.getReferenceId(), saved.getVersion());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraft(UUID draftId) {
        return getDraft(draftId, null);
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraft(UUID draftId, UUID currentUserId) {
        Optional<TripDraft> draftOpt = tripDraftRepository.findById(draftId);
        if (draftOpt.isPresent()) {
            TripDraft draft = draftOpt.get();
            if (draft.getUserId() != null && currentUserId != null && !draft.getUserId().equals(currentUserId)) {
                log.warn("IDOR attempt: User {} attempted to read draft {} owned by {}", currentUserId, draftId, draft.getUserId());
                throw new AccessDeniedException("You do not have permission to access this draft.");
            }
        }
        return draftOpt;
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraftByReference(String referenceId) {
        return tripDraftRepository.findByReferenceIdIgnoreCase(referenceId);
    }
}
