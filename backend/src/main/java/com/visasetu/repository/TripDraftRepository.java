package com.visasetu.repository;

import com.visasetu.domain.entity.TripDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripDraftRepository extends JpaRepository<TripDraft, UUID> {
    Optional<TripDraft> findByReferenceIdIgnoreCase(String referenceId);

    Optional<TripDraft> findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(String applicantEmail);

    Optional<TripDraft> findFirstByApplicantEmailIgnoreCaseAndIsSubmittedFalseOrderByCreatedAtDesc(String applicantEmail);

    Optional<TripDraft> findFirstByUserIdAndIsSubmittedFalseOrderByCreatedAtDesc(UUID userId);

    List<TripDraft> findAllByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(String applicantEmail);

    List<TripDraft> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<TripDraft> findByDraftIdAndUserId(UUID draftId, UUID userId);

    default Optional<TripDraft> findByApplicantEmailIgnoreCase(String applicantEmail) {
        return findFirstByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(applicantEmail);
    }

    default Optional<TripDraft> findByApplicantEmailIgnoreCaseAndIsSubmittedFalse(String applicantEmail) {
        return findFirstByApplicantEmailIgnoreCaseAndIsSubmittedFalseOrderByCreatedAtDesc(applicantEmail);
    }

    default Optional<TripDraft> findByUserIdAndIsSubmittedFalse(UUID userId) {
        return findFirstByUserIdAndIsSubmittedFalseOrderByCreatedAtDesc(userId);
    }
}
