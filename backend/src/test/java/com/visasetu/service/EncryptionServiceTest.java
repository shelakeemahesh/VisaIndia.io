package com.visasetu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("dGhpcy1pcy1hLTMyLWJ5dGUtc2VjcmV0LWtleS0xMjM=");
    }

    @Test
    @DisplayName("encrypt and decrypt roundtrips plaintext correctly")
    void encrypt_decrypt_roundtrip() {
        String sensitivePassport = "A12345678";
        String cipher = encryptionService.encrypt(sensitivePassport);

        assertThat(cipher).isNotNull().isNotEqualTo(sensitivePassport);
        assertThat(encryptionService.decrypt(cipher)).isEqualTo(sensitivePassport);
    }

    @Test
    @DisplayName("decrypt throws SecurityException on corrupted or invalid ciphertext")
    void decrypt_invalidCiphertext_throwsSecurityException() {
        assertThatThrownBy(() -> encryptionService.decrypt("not-valid-base64-or-gcm-payload"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("maskSensitive masks passport number preserving last 3 digits")
    void maskSensitive_preservesLastThree() {
        assertThat(EncryptionService.maskSensitive("TK-8849201")).isEqualTo("••••••201");
        assertThat(EncryptionService.maskSensitive("AB")).isEqualTo("•••");
        assertThat(EncryptionService.maskSensitive(null)).isEmpty();
    }
}
