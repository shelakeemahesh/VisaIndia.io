package com.visasetu.controller;

import com.visasetu.domain.entity.User;
import com.visasetu.repository.UserRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtTokenService jwtTokenService;

    @Data
    public static class RequestOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class VerifyOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        private String otp;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody RequestOtpDto request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Requesting OTP for email: {}", email);

        // Ensure user record exists
        userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .role("ROLE_TRAVELER")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            return userRepository.save(newUser);
        });

        String code = otpService.generateOtp(email);

        return ResponseEntity.ok(Map.of(
                "status", "OTP_SENT",
                "message", "A 6-digit verification code has been dispatched. For local testing, code: " + code,
                "email", email
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpDto request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        boolean isValid = otpService.verifyOtp(email, otp);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired verification code."
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .role("ROLE_TRAVELER")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            return userRepository.save(newUser);
        });

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenService.getExpirationSeconds(),
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                )
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated."));
        }

        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId().toString(),
                        "email", u.getEmail(),
                        "role", u.getRole(),
                        "createdAt", u.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
