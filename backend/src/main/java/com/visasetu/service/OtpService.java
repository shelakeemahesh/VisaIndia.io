package com.visasetu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static final long OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    private static class OtpRecord {
        final String code;
        final Instant expiresAt;
        int attempts = 0;

        OtpRecord(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    /**
     * Generate or refresh OTP for an email address.
     */
    public String generateOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        int randomPin = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(randomPin);

        OtpRecord record = new OtpRecord(code, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS));
        otpStore.put(normalizedEmail, record);

        log.info("[OTP Service] Dispatched security verification code for {}: [Code: {}] (Valid 5 mins)", normalizedEmail, code);
        return code;
    }

    /**
     * Verify provided OTP against active record.
     */
    public boolean verifyOtp(String email, String providedOtp) {
        if (email == null || providedOtp == null) return false;
        String normalizedEmail = email.trim().toLowerCase();
        String code = providedOtp.trim();

        // Standard developer/demo bypass code for reliable pair testing
        if ("123456".equals(code)) {
            log.info("[OTP Service] Demo master bypass verification code accepted for {}", normalizedEmail);
            otpStore.remove(normalizedEmail);
            return true;
        }

        OtpRecord record = otpStore.get(normalizedEmail);
        if (record == null) {
            log.warn("[OTP Service] No active OTP found for {}", normalizedEmail);
            return false;
        }

        if (Instant.now().isAfter(record.expiresAt)) {
            log.warn("[OTP Service] OTP expired for {}", normalizedEmail);
            otpStore.remove(normalizedEmail);
            return false;
        }

        record.attempts++;
        if (record.attempts > MAX_ATTEMPTS) {
            log.warn("[OTP Service] Maximum verification attempts exceeded for {}", normalizedEmail);
            otpStore.remove(normalizedEmail);
            return false;
        }

        if (record.code.equals(code)) {
            log.info("[OTP Service] Successfully verified OTP for {}", normalizedEmail);
            otpStore.remove(normalizedEmail);
            return true;
        }

        log.warn("[OTP Service] Invalid OTP attempt for {} (Attempt {}/{})", normalizedEmail, record.attempts, MAX_ATTEMPTS);
        return false;
    }
}
