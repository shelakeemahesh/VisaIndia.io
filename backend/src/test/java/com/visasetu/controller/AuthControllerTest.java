package com.visasetu.controller;

import com.visasetu.config.CorrelationIdFilter;
import com.visasetu.config.JwtAuthenticationFilter;
import com.visasetu.config.SecurityConfig;
import com.visasetu.domain.entity.User;
import com.visasetu.repository.UserRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private OtpService otpService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("POST /api/v1/auth/request-otp issues verification code")
    void shouldRequestOtpSuccessfully() throws Exception {
        given(userRepository.findByEmailIgnoreCase("traveler@example.com"))
                .willReturn(Optional.empty());
        given(otpService.generateOtp("traveler@example.com"))
                .willReturn("654321");

        mockMvc.perform(post("/api/v1/auth/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_SENT"))
                .andExpect(jsonPath("$.email").value("traveler@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp returns JWT token on valid code")
    void shouldVerifyOtpAndReturnToken() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("traveler@example.com")
                .role("ROLE_TRAVELER")
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        given(otpService.verifyOtp("traveler@example.com", "123456")).willReturn(true);
        given(userRepository.findByEmailIgnoreCase("traveler@example.com")).willReturn(Optional.of(user));
        given(jwtTokenService.generateToken(userId, "traveler@example.com", "ROLE_TRAVELER")).willReturn("jwt.test.token");
        given(jwtTokenService.getExpirationSeconds()).willReturn(86400L);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.test.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("traveler@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp returns 401 on incorrect code")
    void shouldRejectInvalidOtp() throws Exception {
        given(otpService.verifyOtp("traveler@example.com", "000000")).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"traveler@example.com\",\"otp\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }
}
