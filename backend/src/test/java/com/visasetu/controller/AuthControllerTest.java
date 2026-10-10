package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.User;
import com.visasetu.repository.UserRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.visasetu.repository.UserSettingsRepository userSettingsRepository;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockBean
    private OtpService otpService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Test
    @DisplayName("POST /api/v1/auth/request-otp returns generic message without leaking code")
    void shouldRequestOtpSuccessfully() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.existsByEmailIgnoreCase("traveler@example.com")).willReturn(true);
        given(otpService.generateOtp("traveler@example.com", "LOGIN")).willReturn("654321");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_SENT"))
                .andExpect(jsonPath("$.email").value("traveler@example.com"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/signup initiates verification and does not issue immediate JWT")
    void shouldRegisterTravelerAndRequireVerification() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("new.traveler@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("encoded_pwd");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("new.traveler@example.com")
                .fullName("John Doe")
                .role("ROLE_TRAVELER")
                .emailVerified(false)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        given(userRepository.save(any(User.class))).willReturn(user);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.traveler@example.com\",\"fullName\":\"John Doe\",\"password\":\"SecurePass123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-signup-otp activates email and returns JWT")
    void shouldVerifySignupOtpAndReturnToken() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("new.traveler@example.com")
                .fullName("John Doe")
                .role("ROLE_TRAVELER")
                .emailVerified(false)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(otpService.verifyOtp("new.traveler@example.com", "654321", "SIGNUP_VERIFY")).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("new.traveler@example.com")).willReturn(Optional.of(user));
        given(jwtTokenService.generateToken(userId, "new.traveler@example.com", "ROLE_TRAVELER")).willReturn("jwt.signup.token");
        given(jwtTokenService.getExpirationSeconds()).willReturn(86400L);

        mockMvc.perform(post("/api/v1/auth/verify-signup-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.traveler@example.com\",\"otp\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value("jwt.signup.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login rejects wrong password with 401")
    void shouldRejectWrongPassword() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("traveler@example.com")
                .passwordHash("encoded_pwd")
                .role("ROLE_TRAVELER")
                .emailVerified(true)
                .isActive(true)
                .build();

        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("traveler@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("WrongPassword1", "encoded_pwd")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\",\"password\":\"WrongPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login rejects unknown user without silent registration")
    void shouldRejectUnknownUser() throws Exception {
        given(rateLimitingService.tryAcquire(anyString(), anyInt(), anyLong())).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("unknown@example.com")).willReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"AnyPassword1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }
}
