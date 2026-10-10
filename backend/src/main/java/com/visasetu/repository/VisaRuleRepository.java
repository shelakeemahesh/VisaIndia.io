package com.visasetu.repository;

import com.visasetu.domain.entity.VisaRule;
import com.visasetu.domain.enums.VisaPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VisaRuleRepository extends JpaRepository<VisaRule, UUID> {

    @Query("SELECT r FROM VisaRule r WHERE UPPER(r.nationality) = UPPER(:nationality) " +
           "AND r.purpose = :purpose " +
           "AND r.effectiveFrom <= CURRENT_DATE " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= CURRENT_DATE) " +
           "ORDER BY r.isRecommended DESC, r.processingDaysMax ASC")
    List<VisaRule> findActiveRules(String nationality, VisaPurpose purpose);

    @Query("SELECT DISTINCT r.nationality FROM VisaRule r ORDER BY r.nationality ASC")
    List<String> findDistinctNationalities();

    @Query("SELECT DISTINCT r.purpose FROM VisaRule r ORDER BY r.purpose ASC")
    List<VisaPurpose> findDistinctPurposes();
}
