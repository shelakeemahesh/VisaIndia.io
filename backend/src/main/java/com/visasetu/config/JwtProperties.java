package com.visasetu.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "visasetu.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Min(value = 1, message = "JWT expiration hours must be at least 1")
    private long expirationHours = 24;

    private Long expirationSeconds;

    public long getEffectiveExpirationSeconds() {
        if (expirationSeconds != null && expirationSeconds > 0) {
            return expirationSeconds;
        }
        return expirationHours * 3600;
    }
}
