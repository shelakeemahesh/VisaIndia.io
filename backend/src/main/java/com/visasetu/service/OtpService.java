package com.visasetu.service;

import com.visasetu.domain.entity.AuthOtp;
import com.visasetu.repository.AuthOtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static final long OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthOtpRepository authOtpRepository;

    public static class OtpRecord {
        final String hashedCode;
        final String purpose;
        final Instant expiresAt;
        int attempts = 0;

        public OtpRecord(String hashedCode, String purpose, Instant expiresAt) {
            this.hashedCode = hashedCode;
            this.purpose = purpose;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    public OtpService() {
        this.authOtpRepository = null;
    }

    @Autowired(required = false)
    public OtpService(AuthOtpRepository authOtpRepository) {
        this.authOtpRepository = authOtpRepository;
    }

    /**
     * Compute SHA-256 hash of raw OTP code.
     */
    public static String hashOtp(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(otp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256 hash", e);
        }
    }

    /**
     * Generate purpose-bound, cryptographically secure 6-digit OTP.
     */
    public String generateOtp(String email) {
        return generateOtp(email, "LOGIN");
    }

    public String generateOtp(String email, String purpose) {
        String normalizedEmail = email.trim().toLowerCase();
        int randomPin = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(randomPin);
        String hashed = hashOtp(code);

        Instant expiresAt = Instant.now().plusSeconds(OTP_VALIDITY_SECONDS);
        OtpRecord record = new OtpRecord(hashed, purpose, expiresAt);
        otpStore.put(normalizedEmail + ":" + purpose, record);

        if (authOtpRepository != null) {
            try {
                // Invalidate any prior unused OTPs for same email & purpose
                var priorList = authOtpRepository.findAllByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
                for (AuthOtp p : priorList) {
                    if (purpose.equalsIgnoreCase(p.getPurpose()) && Boolean.FALSE.equals(p.getUsed())) {
                        p.setUsed(true);
                        authOtpRepository.save(p);
                    }
                }

                AuthOtp authOtp = AuthOtp.builder()
                        .email(normalizedEmail)
                        .otpCode(hashed)
                        .purpose(purpose)
                        .expiresAt(expiresAt)
                        .used(false)
                        .attempts(0)
                        .createdAt(Instant.now())
                        .build();
                authOtpRepository.save(authOtp);
            } catch (Exception e) {
                log.warn("Could not persist OTP record to DB: {}", e.getMessage());
            }
        }

        // Mask email in production logs to avoid PII exposure; never log raw OTP code
        log.info("[OTP Service] Dispatched security verification code for {} (Purpose: {}, Valid: 5m)",
                maskEmail(normalizedEmail), purpose);
        return code;
    }

    /**
     * Verify OTP against active purpose-bound record.
     */
    public boolean verifyOtp(String email, String providedOtp) {
        return verifyOtp(email, providedOtp, "LOGIN");
    }

    public boolean verifyOtp(String email, String providedOtp, String purpose) {
        if (email == null || providedOtp == null) return false;
        String normalizedEmail = email.trim().toLowerCase();
        String code = providedOtp.trim();
        String storeKey = normalizedEmail + ":" + purpose;
        String providedHash = hashOtp(code);

        // Check persistent store first if available
        if (authOtpRepository != null) {
            try {
                var list = authOtpRepository.findAllByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail);
                for (AuthOtp dbOtp : list) {
                    if (purpose.equalsIgnoreCase(dbOtp.getPurpose()) && Boolean.FALSE.equals(dbOtp.getUsed())
                            && Instant.now().isBefore(dbOtp.getExpiresAt())) {
                        int attempts = (dbOtp.getAttempts() != null ? dbOtp.getAttempts() : 0) + 1;
                        dbOtp.setAttempts(attempts);

                        if (attempts > MAX_ATTEMPTS) {
                            log.warn("[OTP Service] Maximum verification attempts exceeded in DB for {}", maskEmail(normalizedEmail));
                            dbOtp.setUsed(true);
                            authOtpRepository.save(dbOtp);
                            otpStore.remove(storeKey);
                            return false;
                        }

                        boolean matches = MessageDigest.isEqual(
                                dbOtp.getOtpCode().getBytes(StandardCharsets.UTF_8),
                                providedHash.getBytes(StandardCharsets.UTF_8)
                        );

                        if (matches) {
                            log.info("[OTP Service] Successfully verified OTP in DB for {} (Purpose: {})", maskEmail(normalizedEmail), purpose);
                            dbOtp.setUsed(true);
                            authOtpRepository.save(dbOtp);
                            otpStore.remove(storeKey);
                            return true;
                        } else {
                            authOtpRepository.save(dbOtp);
                            log.warn("[OTP Service] Invalid OTP attempt for {} (Attempt {}/{})", maskEmail(normalizedEmail), attempts, MAX_ATTEMPTS);
                            return false;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("DB OTP verification error: {}", e.getMessage());
            }
        }

        // Memory store check (fallback or standalone unit test context)
        OtpRecord record = otpStore.get(storeKey);
        if (record == null) {
            log.warn("[OTP Service] No active OTP found for {} with purpose {}", maskEmail(normalizedEmail), purpose);
            return false;
        }

        if (Instant.now().isAfter(record.expiresAt)) {
            log.warn("[OTP Service] OTP expired for {}", maskEmail(normalizedEmail));
            otpStore.remove(storeKey);
            return false;
        }

        record.attempts++;
        if (record.attempts > MAX_ATTEMPTS) {
            log.warn("[OTP Service] Maximum verification attempts exceeded for {}", maskEmail(normalizedEmail));
            otpStore.remove(storeKey);
            return false;
        }

        boolean matches = MessageDigest.isEqual(
                record.hashedCode.getBytes(StandardCharsets.UTF_8),
                providedHash.getBytes(StandardCharsets.UTF_8)
        );

        if (matches) {
            log.info("[OTP Service] Successfully verified OTP for {} (Purpose: {})", maskEmail(normalizedEmail), purpose);
            otpStore.remove(storeKey);
            return true;
        }

        log.warn("[OTP Service] Invalid OTP attempt for {} (Attempt {}/{})", maskEmail(normalizedEmail), record.attempts, MAX_ATTEMPTS);
        return false;
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "*@" + parts[1];
        return name.substring(0, 2) + "****" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
