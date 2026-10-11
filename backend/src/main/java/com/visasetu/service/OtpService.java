package com.visasetu.service;

import com.visasetu.domain.entity.AuthOtp;
import com.visasetu.domain.enums.OtpPurpose;
import com.visasetu.repository.AuthOtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
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

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${visasetu.security.otp-pepper:visasetu-enterprise-otp-pepper-2026-super-secret}")
    private String otpPepper = "visasetu-enterprise-otp-pepper-2026-super-secret";

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

    // In-memory fallback only for headless standalone unit tests where DB is null
    private final Map<String, OtpRecord> inMemoryFallbackStore = new ConcurrentHashMap<>();

    public OtpService() {
        this.authOtpRepository = null;
    }

    @Autowired(required = false)
    public OtpService(AuthOtpRepository authOtpRepository) {
        this.authOtpRepository = authOtpRepository;
    }

    public void setOtpPepper(String pepper) {
        if (pepper != null && !pepper.isBlank()) {
            this.otpPepper = pepper;
        }
    }

    /**
     * Compute HMAC-SHA256 of raw OTP code with server-side secret pepper.
     */
    public String hashOtpWithPepper(String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    otpPepper.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(otp.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256 for OTP: {}", e.getMessage());
            throw new IllegalStateException("Failed to calculate HMAC peppered hash", e);
        }
    }

    /**
     * Legacy SHA-256 hash compatibility.
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
        return generateOtp(email, OtpPurpose.LOGIN.name());
    }

    @Transactional
    public String generateOtp(String email, String rawPurpose) {
        OtpPurpose normalizedPurpose = OtpPurpose.fromString(rawPurpose);
        String purposeStr = normalizedPurpose.name();
        String normalizedEmail = email.trim().toLowerCase();

        int randomPin = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(randomPin);
        String pepperedHash = hashOtpWithPepper(code);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(OTP_VALIDITY_SECONDS);

        if (authOtpRepository != null) {
            try {
                // Delete expired rows first to keep table clean
                authOtpRepository.deleteByExpiresAtBefore(now);

                // Invalidate all prior unused OTPs for the same (email, purpose)
                var priorList = authOtpRepository.findAllByEmailIgnoreCaseAndPurposeAndUsedFalse(normalizedEmail, purposeStr);
                for (AuthOtp prior : priorList) {
                    prior.setUsed(true);
                }
                if (!priorList.isEmpty()) {
                    authOtpRepository.saveAll(priorList);
                }

                AuthOtp authOtp = AuthOtp.builder()
                        .email(normalizedEmail)
                        .otpCode(pepperedHash)
                        .purpose(purposeStr)
                        .expiresAt(expiresAt)
                        .used(false)
                        .attempts(0)
                        .createdAt(now)
                        .build();
                authOtpRepository.save(authOtp);
            } catch (Exception e) {
                log.warn("Could not persist OTP record to database: {}", e.getMessage());
                // Fallback to in-memory store if DB persistence fails
                inMemoryFallbackStore.put(normalizedEmail + ":" + purposeStr, new OtpRecord(pepperedHash, purposeStr, expiresAt));
            }
        } else {
            inMemoryFallbackStore.put(normalizedEmail + ":" + purposeStr, new OtpRecord(pepperedHash, purposeStr, expiresAt));
        }

        // Send email via JavaMailSender (MailHog in dev/test, SMTP in production)
        sendOtpEmail(normalizedEmail, code, purposeStr);

        log.info("[OTP Service] Dispatched security verification code for {} (Purpose: {}, Valid: 5m)",
                maskEmail(normalizedEmail), purposeStr);
        return code;
    }

    private void sendOtpEmail(String email, String code, String purpose) {
        if (mailSender == null) {
            log.debug("JavaMailSender not configured; skipping email dispatch to {}", maskEmail(email));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@visasetu.internal");
            message.setTo(email);
            message.setSubject("VisaSetu Security Verification Code - " + purpose);
            message.setText("Your one-time verification code is: " + code + "\n\n" +
                    "This code is purpose-bound for " + purpose + " and is valid for 5 minutes.\n" +
                    "If you did not request this code, please ignore this email.");
            mailSender.send(message);
            log.info("[OTP Service] Successfully dispatched verification email to {}", maskEmail(email));
        } catch (Exception e) {
            log.warn("[OTP Service] Failed to send email via JavaMailSender: {}", e.getMessage());
        }
    }

    /**
     * Verify OTP against active purpose-bound record.
     */
    public boolean verifyOtp(String email, String providedOtp) {
        return verifyOtp(email, providedOtp, OtpPurpose.LOGIN.name());
    }

    @Transactional
    public boolean verifyOtp(String email, String providedOtp, String rawPurpose) {
        if (email == null || providedOtp == null) return false;
        OtpPurpose normalizedPurpose;
        try {
            normalizedPurpose = OtpPurpose.fromString(rawPurpose);
        } catch (IllegalArgumentException e) {
            log.warn("[OTP Service] Verification rejected for invalid purpose: {}", rawPurpose);
            return false;
        }

        String purposeStr = normalizedPurpose.name();
        String normalizedEmail = email.trim().toLowerCase();
        String code = providedOtp.trim();
        String candidatePepperedHash = hashOtpWithPepper(code);
        String candidateSha256Hash = hashOtp(code);
        Instant now = Instant.now();

        // 1. Primary check: Database store
        if (authOtpRepository != null) {
            try {
                var activeOpt = authOtpRepository.findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        normalizedEmail, purposeStr, now
                );

                if (activeOpt.isPresent()) {
                    AuthOtp dbOtp = activeOpt.get();
                    int attempts = (dbOtp.getAttempts() != null ? dbOtp.getAttempts() : 0) + 1;
                    dbOtp.setAttempts(attempts);

                    if (attempts > MAX_ATTEMPTS) {
                        log.warn("[OTP Service] Maximum verification attempts exceeded in DB for {}", maskEmail(normalizedEmail));
                        dbOtp.setUsed(true);
                        authOtpRepository.save(dbOtp);
                        return false;
                    }

                    boolean matches = MessageDigest.isEqual(
                            dbOtp.getOtpCode().getBytes(StandardCharsets.UTF_8),
                            candidatePepperedHash.getBytes(StandardCharsets.UTF_8)
                    ) || MessageDigest.isEqual(
                            dbOtp.getOtpCode().getBytes(StandardCharsets.UTF_8),
                            candidateSha256Hash.getBytes(StandardCharsets.UTF_8)
                    );

                    if (matches) {
                        log.info("[OTP Service] Successfully verified OTP in DB for {} (Purpose: {})", maskEmail(normalizedEmail), purposeStr);
                        dbOtp.setUsed(true);
                        authOtpRepository.save(dbOtp);
                        return true;
                    } else {
                        authOtpRepository.save(dbOtp);
                        log.warn("[OTP Service] Invalid OTP attempt for {} (Attempt {}/{})", maskEmail(normalizedEmail), attempts, MAX_ATTEMPTS);
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("Database OTP verification error: {}", e.getMessage());
            }
        }

        // 2. In-memory fallback (only for tests or when DB is unavailable)
        String storeKey = normalizedEmail + ":" + purposeStr;
        OtpRecord record = inMemoryFallbackStore.get(storeKey);
        if (record == null) {
            log.warn("[OTP Service] No active OTP found for {} with purpose {}", maskEmail(normalizedEmail), purposeStr);
            return false;
        }

        if (now.isAfter(record.expiresAt)) {
            log.warn("[OTP Service] OTP expired for {}", maskEmail(normalizedEmail));
            inMemoryFallbackStore.remove(storeKey);
            return false;
        }

        record.attempts++;
        if (record.attempts > MAX_ATTEMPTS) {
            log.warn("[OTP Service] Maximum verification attempts exceeded for {}", maskEmail(normalizedEmail));
            inMemoryFallbackStore.remove(storeKey);
            return false;
        }

        boolean matches = MessageDigest.isEqual(
                record.hashedCode.getBytes(StandardCharsets.UTF_8),
                candidatePepperedHash.getBytes(StandardCharsets.UTF_8)
        ) || MessageDigest.isEqual(
                record.hashedCode.getBytes(StandardCharsets.UTF_8),
                candidateSha256Hash.getBytes(StandardCharsets.UTF_8)
        );

        if (matches) {
            log.info("[OTP Service] Successfully verified OTP for {} (Purpose: {})", maskEmail(normalizedEmail), purposeStr);
            inMemoryFallbackStore.remove(storeKey);
            return true;
        }

        log.warn("[OTP Service] Invalid OTP attempt for {} (Attempt {}/{})", maskEmail(normalizedEmail), record.attempts, MAX_ATTEMPTS);
        return false;
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name.charAt(0) + "*@" + parts[1];
        return name.substring(0, 2) + "****" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
