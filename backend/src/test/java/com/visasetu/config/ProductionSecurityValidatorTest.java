package com.visasetu.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecurityValidatorTest {

    @Test
    @DisplayName("Local profile allows development default secrets")
    void localProfile_allowsDevDefaults() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", true);
        ReflectionTestUtils.setField(validator, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test profile allows test configuration")
    void testProfile_allowsTestConfig() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Outside local profile aborts if JWT secret is missing or default")
    void outsideLocal_abortsOnDefaultOrMissingJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(validator, "piiEncryptionKey", "custom-valid-pii-key-1234567890");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_custom_123");
        ReflectionTestUtils.setField(validator, "razorpayKeySecret", "rzp_secret_123");
        ReflectionTestUtils.setField(validator, "razorpayWebhookSecret", "rzp_hook_123");

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Default, weak, or missing JWT secret");
    }

    @Test
    @DisplayName("Outside local profile aborts if Razorpay credentials are missing")
    void outsideLocal_abortsOnMissingRazorpayCredentials() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "jwtSecret", "SecureCustomEnterpriseSecretKeyForProductionMustBeVeryLongAndSecure2026");
        ReflectionTestUtils.setField(validator, "piiEncryptionKey", "SecureCustomPiiEncryptionKeyForProd32Chars!");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "");

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing RAZORPAY_KEY_ID outside local profile");
    }

    @Test
    @DisplayName("Production profile aborts if mock adjudication is enabled")
    void prodProfile_abortsOnMockAdjudication() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", true);
        ReflectionTestUtils.setField(validator, "jwtSecret", "SecureCustomEnterpriseSecretKeyForProductionMustBeVeryLongAndSecure2026");
        ReflectionTestUtils.setField(validator, "piiEncryptionKey", "SecureCustomPiiEncryptionKeyForProd32Chars!");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_live_real_12345");
        ReflectionTestUtils.setField(validator, "razorpayKeySecret", "real_secret_45678");
        ReflectionTestUtils.setField(validator, "razorpayWebhookSecret", "real_webhook_secret_9999");

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mock government adjudication cannot be enabled in production");
    }

    @Test
    @DisplayName("Production profile passes when secure custom secrets are configured")
    void prodProfile_passesWithSecureSecrets() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "SecureCustomEnterpriseSecretKeyForProductionMustBeVeryLongAndSecure2026");
        ReflectionTestUtils.setField(validator, "piiEncryptionKey", "SecureCustomPiiEncryptionKeyForProd32Chars!");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_live_production_key_12345");
        ReflectionTestUtils.setField(validator, "razorpayKeySecret", "production_secret_67890");
        ReflectionTestUtils.setField(validator, "razorpayWebhookSecret", "production_webhook_secret_abcde");

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
