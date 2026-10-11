package com.visasetu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visasetu.domain.entity.TripDraft;
import com.visasetu.repository.TripDraftRepository;
import com.visasetu.repository.TripRepository;
import com.visasetu.security.OwnershipPolicy;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripDraftService {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final TripDraftRepository tripDraftRepository;
    private final TripRepository tripRepository;
    private final OwnershipPolicy ownershipPolicy;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TripDraft createOrGetDraft(String email, String nationality, String categoryName) {
        return createOrGetDraft(null, email, nationality, categoryName);
    }

    @Transactional
    public TripDraft createOrGetDraft(UUID userId, String email, String nationality, String categoryName) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        // 1. Authenticated user: STRICTLY isolated by userId to prevent draft cross-contamination
        if (userId != null) {
            Optional<TripDraft> userDraft = tripDraftRepository.findFirstByUserIdAndIsSubmittedFalseOrderByCreatedAtDesc(userId);
            if (userDraft.isPresent()) {
                return userDraft.get();
            }
        } else if (!normalizedEmail.isBlank()) {
            // 2. Guest user: ONLY check unowned drafts belonging to this email
            Optional<TripDraft> guestDraft = tripDraftRepository
                    .findFirstByApplicantEmailIgnoreCaseAndIsSubmittedFalseOrderByCreatedAtDesc(normalizedEmail)
                    .filter(d -> d.getUserId() == null);
            if (guestDraft.isEmpty()) {
                guestDraft = tripDraftRepository.findByApplicantEmailIgnoreCase(normalizedEmail)
                        .filter(d -> d.getUserId() == null && !Boolean.TRUE.equals(d.getIsSubmitted()));
            }

            if (guestDraft.isPresent()) {
                TripDraft existing = guestDraft.get();
                if (existing.getGuestAccessToken() == null) {
                    byte[] tokenBytes = new byte[32];
                    secureRandom.nextBytes(tokenBytes);
                    existing.setGuestAccessToken(HexFormat.of().formatHex(tokenBytes));
                    TripDraft saved = tripDraftRepository.save(existing);
                    return saved != null ? saved : existing;
                }
                return existing;
            }
        }

        // 3. Create fresh draft with unique VS-2026-XXXXXXXXXX reference ID
        String refId = generateUniqueReferenceId();
        String resolvedNationality = (nationality != null && !nationality.isBlank()) ? nationality.trim() : "USA";
        String resolvedCategory = (categoryName != null && !categoryName.isBlank()) ? categoryName.trim() : "e-Tourist Visa (30 Days)";

        String initialProfile;
        String initialModules;
        try {
            initialProfile = objectMapper.writeValueAsString(Map.of(
                    "nationality", resolvedNationality,
                    "categoryName", resolvedCategory
            ));
            initialModules = objectMapper.writeValueAsString(Map.of(
                    "eVisa", Map.of("status", "DRAFT"),
                    "eArrival", Map.of("status", "DRAFT"),
                    "eSuvidha", Map.of("status", "DRAFT")
            ));
        } catch (Exception e) {
            initialProfile = "{\"nationality\":\"" + resolvedNationality + "\",\"categoryName\":\"" + resolvedCategory + "\"}";
            initialModules = "{\"eVisa\":{\"status\":\"DRAFT\"},\"eArrival\":{\"status\":\"DRAFT\"},\"eSuvidha\":{\"status\":\"DRAFT\"}}";
        }

        String guestToken = null;
        if (userId == null) {
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            guestToken = HexFormat.of().formatHex(tokenBytes);
        }

        TripDraft draft = TripDraft.builder()
                .referenceId(refId)
                .userId(userId)
                .guestAccessToken(guestToken)
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
        log.info("Initialized new TripDraft {} for user {} (Email: {})", refId, userId, OtpService.maskEmail(normalizedEmail));
        return saved;
    }

    private String generateUniqueReferenceId() {
        for (int i = 0; i < 15; i++) {
            StringBuilder sb = new StringBuilder("VS-2026-");
            for (int j = 0; j < 10; j++) {
                sb.append(ALPHANUMERIC.charAt(secureRandom.nextInt(ALPHANUMERIC.length())));
            }
            String candidate = sb.toString();
            if (tripDraftRepository.findByReferenceIdIgnoreCase(candidate).isEmpty()
                    && tripRepository.findByReferenceIdIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "VS-2026-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void validateDraftAccess(TripDraft draft, UUID currentUserId, String guestAccessToken, boolean isWrite) {
        if (draft.getUserId() != null) {
            ownershipPolicy.assertOwner(currentUserId, draft.getUserId());
        } else {
            // Unowned guest draft: authenticated user claiming draft on write
            if (currentUserId != null && isWrite) {
                draft.setUserId(currentUserId);
                return;
            }
            // If guest token is configured, enforce matching token
            if (draft.getGuestAccessToken() != null && guestAccessToken != null) {
                boolean matches = MessageDigest.isEqual(
                        draft.getGuestAccessToken().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        guestAccessToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );
                if (!matches) {
                    log.warn("IDOR attempt: Mismatched guest access token for draft {}", draft.getDraftId());
                    throw new AccessDeniedException("Invalid or missing guest access token.");
                }
            } else if (draft.getGuestAccessToken() != null && currentUserId == null) {
                log.warn("IDOR attempt: Missing guest access token for unowned draft {}", draft.getDraftId());
                throw new AccessDeniedException("Invalid or missing guest access token.");
            }
        }
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        return autosaveDraft(draftId, null, null, expectedVersion, sharedProfileJson, modulesJson, step);
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, UUID currentUserId, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        return autosaveDraft(draftId, currentUserId, null, expectedVersion, sharedProfileJson, modulesJson, step);
    }

    @Transactional
    public TripDraft autosaveDraft(UUID draftId, UUID currentUserId, String guestAccessToken, Long expectedVersion, String sharedProfileJson, String modulesJson, Integer step) {
        TripDraft draft = tripDraftRepository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft not found for ID: " + draftId));

        validateDraftAccess(draft, currentUserId, guestAccessToken, true);

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
        return getDraft(draftId, null, null);
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraft(UUID draftId, UUID currentUserId) {
        return getDraft(draftId, currentUserId, null);
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraft(UUID draftId, UUID currentUserId, String guestAccessToken) {
        Optional<TripDraft> draftOpt = tripDraftRepository.findById(draftId);
        draftOpt.ifPresent(draft -> validateDraftAccess(draft, currentUserId, guestAccessToken, false));
        return draftOpt;
    }

    @Transactional(readOnly = true)
    public Optional<TripDraft> getDraftByReference(String referenceId) {
        return tripDraftRepository.findByReferenceIdIgnoreCase(referenceId);
    }
}
