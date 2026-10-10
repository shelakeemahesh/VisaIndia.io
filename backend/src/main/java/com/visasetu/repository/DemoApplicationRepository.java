package com.visasetu.repository;

import com.visasetu.domain.entity.DemoApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DemoApplicationRepository extends JpaRepository<DemoApplication, UUID> {
    Optional<DemoApplication> findByReferenceIdIgnoreCaseAndApplicantEmailIgnoreCase(String referenceId, String applicantEmail);
    Optional<DemoApplication> findByReferenceIdIgnoreCase(String referenceId);
}
