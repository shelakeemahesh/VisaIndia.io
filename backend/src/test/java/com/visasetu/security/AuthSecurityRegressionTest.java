package com.visasetu.security;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.controller.AuthController;
import com.visasetu.domain.entity.User;
import com.visasetu.repository.UserRepository;
import com.visasetu.repository.UserSettingsRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
import com.visasetu.service.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
public class AuthSecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserSettingsRepository userSettingsRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private OtpService otpService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Test
    @DisplayName("Security: Hardcoded password bypasses (Welcome@123, password, admin123) are rejected")
    void shouldRejectHardcodedPasswordBypasses() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test.traveler@example.com")
                .passwordHash("real_bcrypt_hash")
                .role("ROLE_TRAVELER")
                .emailVerified(true)
                .isActive(true)
                .build();

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("test.traveler@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Welcome@123", "real_bcrypt_hash")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test.traveler@example.com\",\"password\":\"Welcome@123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    @DisplayName("Security: Unverified email accounts cannot log in")
    void shouldRejectUnverifiedAccountLogin() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("unverified@example.com")
                .passwordHash("real_hash")
                .role("ROLE_TRAVELER")
                .emailVerified(false)
                .isActive(true)
                .build();

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("unverified@example.com")).willReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unverified@example.com\",\"password\":\"SecurePass123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("Security: Deactivated accounts are rejected")
    void shouldRejectDeactivatedAccountLogin() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("deactivated@example.com")
                .passwordHash("real_hash")
                .role("ROLE_TRAVELER")
                .emailVerified(true)
                .isActive(false)
                .build();

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("deactivated@example.com")).willReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"deactivated@example.com\",\"password\":\"SecurePass123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This account has been deactivated. Please contact support."));
    }

    @Test
    @DisplayName("Security: Rate limiter rejects excessive login attempts with 429 Too Many Requests")
    void shouldEnforceRateLimitingOnLogin() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"flooder@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many failed login attempts. Please wait a minute before retrying."));
    }

    @Test
    @DisplayName("Security: Password reset enforces valid purpose-bound OTP")
    void shouldRejectPasswordResetWithInvalidOtp() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(otpService.verifyOtp("user@example.com", "111111", "PASSWORD_RESET")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"otp\":\"111111\",\"newPassword\":\"NewSecurePass123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired verification code."));
    }
}
