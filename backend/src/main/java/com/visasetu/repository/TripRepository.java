package com.visasetu.repository;

import com.visasetu.domain.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    Optional<Trip> findByReferenceIdIgnoreCase(String referenceId);
    Optional<Trip> findByReferenceIdIgnoreCaseAndUserId(String referenceId, UUID userId);
    List<Trip> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Trip> findByApplicantEmailIgnoreCaseOrderByCreatedAtDesc(String applicantEmail);
    List<Trip> findByStatusOrderByCreatedAtAsc(String status);
}
