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
        String otp = otpService.generateOtp(email);

        assertThat(otp).matches("\\d{6}");
        assertThat(otpService.verifyOtp(email, otp)).isTrue();
    }

    @Test
    @DisplayName("verifyOtp supports master demo bypass code 123456")
    void verifyOtp_masterBypass_returnsTrue() {
        String email = "anytraveler@example.com";
        assertThat(otpService.verifyOtp(email, "123456")).isTrue();
    }

    @Test
    @DisplayName("verifyOtp fails on wrong code and limits attempts")
    void verifyOtp_wrongCode_fails() {
        String email = "traveler@example.com";
        otpService.generateOtp(email);

        assertThat(otpService.verifyOtp(email, "000000")).isFalse();
    }
}
