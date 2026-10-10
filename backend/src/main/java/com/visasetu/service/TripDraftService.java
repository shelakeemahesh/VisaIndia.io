package com.visasetu.service;

import com.visasetu.domain.entity.TripDraft;
import com.visasetu.repository.TripDraftRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        String normalizedEmail = email.trim().toLowerCase();

        return tripDraftRepository.findByApplicantEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            String refId = "VS-" + Year.now().getValue() + "-" + (1000 + secureRandom.nextInt(9000));

            String initialProfile = "{\"nationality\":\"" + nationality + "\",\"categoryName\":\"" + categoryName + "\"}";
            String initialModules = "{\"eVisa\":{\"status\":\"DRAFT\"},\"eArrival\":{\"status\":\"DRAFT\"},\"eSuvidha\":{\"status\":\"DRAFT\"}}";

            TripDraft draft = TripDraft.builder()
                    .referenceId(refId)
                    .applicantEmail(normalizedEmail)
                    .currentStep(1)
                    .sharedProfileJson(initialProfile)
                    .modulesJson(initialModules)
                    .version(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            TripDraft saved = tripDraftRepository.save(draft);
            log.info("Initialized new TripDraft {} for email {}", refId, normalizedEmail);
            return saved;
        });
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        TripDraft draft = tripDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found for ID: " + draftId));

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
        return tripDraftRepository.findById(draftId);
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraftByReference(String referenceId) {
        return tripDraftRepository.findByReferenceIdIgnoreCase(referenceId);
    }
}
