package com.visasetu.repository;

import com.visasetu.domain.entity.AuthOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthOtpRepository extends JpaRepository<AuthOtp, UUID> {
    Optional<AuthOtp> findTopByEmailIgnoreCaseAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, String otpCode, Instant now
    );
    List<AuthOtp> findAllByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
