package com.visasetu.domain.enums;

/**
 * Normalized purpose types for One-Time Passwords (OTPs).
 */
public enum OtpPurpose {
    LOGIN,
    SIGNUP_VERIFY,
    PASSWORD_RESET,
    TRACKING;

    public static OtpPurpose fromString(String value) {
        if (value == null || value.isBlank()) {
            return LOGIN;
        }
        try {
            return OtpPurpose.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported OTP purpose: " + value + ". Allowed: LOGIN, SIGNUP_VERIFY, PASSWORD_RESET, TRACKING");
        }
    }
}
