package com.visasetu.service;

import com.visasetu.domain.entity.AuthOtp;
import com.visasetu.repository.AuthOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpServiceDbTest {

    @Mock
    private AuthOtpRepository authOtpRepository;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(authOtpRepository);
        otpService.setOtpPepper("test-otp-pepper-1234567890");
    }

    @Test
    @DisplayName("generateOtp invalidates prior unused OTPs for same email and purpose and saves new OTP")
    void generateOtp_invalidatesPriorAndPersists() {
        String email = "traveler@example.com";
        AuthOtp prior = AuthOtp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .purpose("LOGIN")
                .used(false)
                .build();

        given(authOtpRepository.findAllByEmailIgnoreCaseAndPurposeAndUsedFalse(email, "LOGIN"))
                .willReturn(List.of(prior));

        String code = otpService.generateOtp(email, "LOGIN");

        assertThat(code).matches("\\d{6}");
        assertThat(prior.getUsed()).isTrue();
        verify(authOtpRepository).save(any(AuthOtp.class));
    }

    @Test
    @DisplayName("verifyOtp in DB counts attempts and locks out after max attempts exceeded")
    void verifyOtp_countsAttemptsAndLocksOut() {
        String email = "traveler@example.com";
        String code = "654321";
        String pepperedHash = otpService.hashOtpWithPepper(code);

        AuthOtp dbOtp = AuthOtp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .purpose("LOGIN")
                .otpCode(pepperedHash)
                .attempts(4)
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .build();

        given(authOtpRepository.findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(email), eq("LOGIN"), any(Instant.class)
        )).willReturn(Optional.of(dbOtp));

        // Wrong attempt 5
        boolean attempt5 = otpService.verifyOtp(email, "000000", "LOGIN");
        assertThat(attempt5).isFalse();
        assertThat(dbOtp.getAttempts()).isEqualTo(5);
        assertThat(dbOtp.getUsed()).isFalse();

        // 6th attempt exceeds max 5 -> lockout & marked used
        boolean attempt6 = otpService.verifyOtp(email, "000000", "LOGIN");
        assertThat(attempt6).isFalse();
        assertThat(dbOtp.getAttempts()).isEqualTo(6);
        assertThat(dbOtp.getUsed()).isTrue();
    }

    @Test
    @DisplayName("verifyOtp in DB verifies successfully and marks OTP used")
    void verifyOtp_successfulMatch_marksUsed() {
        String email = "traveler@example.com";
        String code = "123456";
        String pepperedHash = otpService.hashOtpWithPepper(code);

        AuthOtp dbOtp = AuthOtp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .purpose("LOGIN")
                .otpCode(pepperedHash)
                .attempts(0)
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .build();

        given(authOtpRepository.findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(email), eq("LOGIN"), any(Instant.class)
        )).willReturn(Optional.of(dbOtp));

        boolean matched = otpService.verifyOtp(email, code, "LOGIN");

        assertThat(matched).isTrue();
        assertThat(dbOtp.getUsed()).isTrue();
        assertThat(dbOtp.getAttempts()).isEqualTo(1);
    }
}
