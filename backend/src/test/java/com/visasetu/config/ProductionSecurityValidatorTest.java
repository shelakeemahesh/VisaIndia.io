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
    @DisplayName("Dev profile allows mock adjudication and development secrets")
    void devProfile_allowsDevDefaults() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", true);
        ReflectionTestUtils.setField(validator, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Production profile aborts if mock adjudication is enabled")
    void prodProfile_abortsOnMockAdjudication() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", true);

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mock government adjudication cannot be enabled in production");
    }

    @Test
    @DisplayName("Production profile aborts on default JWT secret")
    void prodProfile_abortsOnDefaultJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "mockAdjudicationEnabled", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Default or weak JWT secret detected");
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

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
