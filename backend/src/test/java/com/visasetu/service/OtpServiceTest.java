package com.visasetu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceTest {

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
    }

    @Test
    @DisplayName("generateOtp returns 6-digit pin and validates successfully")
    void generateOtp_validatesSuccessfully() {
        String email = "traveler@example.com";
        String otp = otpService.generateOtp(email, "LOGIN");

        assertThat(otp).matches("\\d{6}");
        assertThat(otpService.verifyOtp(email, otp, "LOGIN")).isTrue();
    }

    @Test
    @DisplayName("verifyOtp strictly rejects hardcoded master demo bypass code 123456")
    void verifyOtp_masterBypass_rejected() {
        String email = "anytraveler@example.com";
        assertThat(otpService.verifyOtp(email, "123456", "LOGIN")).isFalse();
    }

    @Test
    @DisplayName("verifyOtp enforces purpose-binding (SIGNUP_VERIFY cannot verify LOGIN)")
    void verifyOtp_purposeMismatch_rejected() {
        String email = "traveler@example.com";
        String otp = otpService.generateOtp(email, "SIGNUP_VERIFY");

        assertThat(otpService.verifyOtp(email, otp, "LOGIN")).isFalse();
        assertThat(otpService.verifyOtp(email, otp, "SIGNUP_VERIFY")).isTrue();
    }

    @Test
    @DisplayName("verifyOtp enforces single-use consumption")
    void verifyOtp_singleUse_consumed() {
        String email = "traveler@example.com";
        String otp = otpService.generateOtp(email, "LOGIN");

        assertThat(otpService.verifyOtp(email, otp, "LOGIN")).isTrue();
        // Second attempt must fail because OTP was consumed
        assertThat(otpService.verifyOtp(email, otp, "LOGIN")).isFalse();
    }

    @Test
    @DisplayName("verifyOtp fails on wrong code and limits attempts")
    void verifyOtp_wrongCode_fails() {
        String email = "traveler@example.com";
        otpService.generateOtp(email, "LOGIN");

        assertThat(otpService.verifyOtp(email, "000000", "LOGIN")).isFalse();
    }
}
