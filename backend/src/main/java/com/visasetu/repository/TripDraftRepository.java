package com.visasetu.repository;

import com.visasetu.domain.entity.TripDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripDraftRepository extends JpaRepository<TripDraft, UUID> {
    Optional<TripDraft> findByReferenceIdIgnoreCase(String referenceId);
    Optional<TripDraft> findByApplicantEmailIgnoreCase(String applicantEmail);
}
