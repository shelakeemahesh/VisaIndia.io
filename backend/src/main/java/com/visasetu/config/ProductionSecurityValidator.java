package com.visasetu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionSecurityValidator implements ApplicationRunner {

    private final Environment environment;

    @Value("${visasetu.mock.adjudication.enabled:false}")
    private boolean mockAdjudicationEnabled;

    @Value("${visasetu.jwt.secret:}")
    private String jwtSecret;

    @Value("${visasetu.security.pii-encryption-key:}")
    private String piiEncryptionKey;

    @Value("${visasetu.razorpay.key-id:}")
    private String razorpayKeyId;

    private static final String DEFAULT_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String LEGACY_DEFAULT_JWT_SECRET = "VisaSetuEnterpriseSecretKeyForJwtSigningMustBeAtLeast256BitsLong2026";
    private static final String DEFAULT_PII_KEY = "dGhpcy1pcy1hLTMyLWJ5dGUtc2VjcmV0LWtleS0xMjM=";

    @Override
    public void run(ApplicationArguments args) {
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        if (isProduction) {
            log.info("Production profile detected. Running enterprise security configuration validation...");

            if (mockAdjudicationEnabled) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Mock government adjudication cannot be enabled in production environments.");
            }

            if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.equals(DEFAULT_JWT_SECRET)
                    || jwtSecret.equals(LEGACY_DEFAULT_JWT_SECRET) || jwtSecret.length() < 32) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Default or weak JWT secret detected in production environment.");
            }

            if (piiEncryptionKey == null || piiEncryptionKey.isBlank() || piiEncryptionKey.equals(DEFAULT_PII_KEY)) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Default PII encryption key detected in production environment.");
            }

            if (razorpayKeyId != null && razorpayKeyId.startsWith("rzp_test_sample")) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Sample test Razorpay key detected in production environment.");
            }

            log.info("Enterprise production security configuration validation passed successfully.");
        }
    }
}
