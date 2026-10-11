package com.visasetu.repository;

import com.visasetu.domain.entity.AuthOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthOtpRepository extends JpaRepository<AuthOtp, UUID> {

    Optional<AuthOtp> findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String purpose, Instant now
    );

    List<AuthOtp> findAllByEmailIgnoreCaseAndPurposeAndUsedFalse(String email, String purpose);

    List<AuthOtp> findAllByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM AuthOtp o WHERE o.expiresAt < :threshold")
    int deleteExpiredOtps(@Param("threshold") Instant threshold);

    long deleteByExpiresAtBefore(Instant threshold);
}
