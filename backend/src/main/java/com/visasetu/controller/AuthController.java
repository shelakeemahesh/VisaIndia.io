package com.visasetu.controller;

import com.visasetu.domain.entity.AuthOtp;
import com.visasetu.domain.entity.User;
import com.visasetu.domain.entity.UserSettings;
import com.visasetu.repository.AuthOtpRepository;
import com.visasetu.repository.UserRepository;
import com.visasetu.repository.UserSettingsRepository;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final AuthOtpRepository authOtpRepository;
    private final OtpService otpService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    @Data
    public static class SignupRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Password is required")
        private String password;

        private String nationality;
        private String passportNumber;
        private String phoneNumber;
    }

    @Data
    public static class LoginRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String password;
        private String otp;
    }

    @Data
    public static class RequestOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String purpose; // "LOGIN", "PASSWORD_RESET"
    }

    @Data
    public static class VerifyOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        private String otp;
    }

    @Data
    public static class ResetPasswordDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        private String otp;

        @NotBlank(message = "New password is required")
        private String newPassword;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing signup for email: {}", email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "An account with this email address already exists. Please log in."
            ));
        }

        User newUser = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword().trim()))
                .nationality(request.getNationality() != null ? request.getNationality().trim() : null)
                .passportNumber(request.getPassportNumber() != null ? request.getPassportNumber().trim() : null)
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role("ROLE_TRAVELER")
                .isActive(true)
                .emailVerified(true)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();

        User savedUser = userRepository.save(newUser);

        UserSettings defaultSettings = UserSettings.builder()
                .user(savedUser)
                .emailNotificationsEnabled(true)
                .arrivalRemindersEnabled(true)
                .advisoryAlertsEnabled(true)
                .autoResyncEnabled(true)
                .languagePreference("en")
                .updatedAt(Instant.now())
                .build();
        userSettingsRepository.save(defaultSettings);

        String token = jwtTokenService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "SUCCESS",
                "message", "Account registered successfully.",
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenService.getExpirationSeconds(),
                "user", Map.of(
                        "id", savedUser.getId().toString(),
                        "email", savedUser.getEmail(),
                        "fullName", savedUser.getFullName(),
                        "nationality", savedUser.getNationality() != null ? savedUser.getNationality() : "",
                        "passportNumber", savedUser.getPassportNumber() != null ? savedUser.getPassportNumber() : "",
                        "role", savedUser.getRole()
                )
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing login for email: {}", email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        // If user doesn't exist yet, we check if demo user or create them for frictionless access
        if (user == null) {
            // For testing & frictionless demo access
            user = User.builder()
                    .email(email)
                    .fullName(email.split("@")[0])
                    .passwordHash(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
                    .role("ROLE_TRAVELER")
                    .isActive(true)
                    .emailVerified(true)
                    .createdAt(Instant.now())
                    .lastLoginAt(Instant.now())
                    .build();
            user = userRepository.save(user);

            UserSettings settings = UserSettings.builder()
                    .user(user)
                    .build();
            userSettingsRepository.save(settings);
        }

        // Authentication by OTP if provided
        if (request.getOtp() != null && !request.getOtp().isBlank()) {
            boolean isValidOtp = otpService.verifyOtp(email, request.getOtp().trim());
            if (!isValidOtp) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid or expired 6-digit OTP code."
                ));
            }
        } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
            // Password verification: allow matching hash OR test demo password "Welcome@123" / "password"
            String pwd = request.getPassword().trim();
            boolean pwdMatches = user.getPasswordHash() != null && passwordEncoder.matches(pwd, user.getPasswordHash());
            boolean isDemoBypass = "Welcome@123".equals(pwd) || "password".equals(pwd) || "admin123".equals(pwd);

            if (!pwdMatches && !isDemoBypass && user.getPasswordHash() != null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Incorrect password. You can also sign in using instant Email OTP."
                ));
            }
            if (user.getPasswordHash() == null) {
                user.setPasswordHash(passwordEncoder.encode(pwd));
            }
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());

        UserSettings settings = userSettingsRepository.findById(user.getId()).orElse(null);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenService.getExpirationSeconds(),
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "nationality", user.getNationality() != null ? user.getNationality() : "",
                        "passportNumber", user.getPassportNumber() != null ? user.getPassportNumber() : "",
                        "role", user.getRole()
                ),
                "settings", settings != null ? Map.of(
                        "emailNotificationsEnabled", settings.getEmailNotificationsEnabled(),
                        "arrivalRemindersEnabled", settings.getArrivalRemindersEnabled(),
                        "advisoryAlertsEnabled", settings.getAdvisoryAlertsEnabled(),
                        "autoResyncEnabled", settings.getAutoResyncEnabled(),
                        "languagePreference", settings.getLanguagePreference()
                ) : Map.of()
        ));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody RequestOtpDto request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Requesting OTP for email: {}", email);

        // Ensure user record exists
        userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .fullName(email.split("@")[0])
                    .role("ROLE_TRAVELER")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            User saved = userRepository.save(newUser);
            userSettingsRepository.save(UserSettings.builder().user(saved).build());
            return saved;
        });

        String code = otpService.generateOtp(email);

        // Record in auth_otp table
        AuthOtp authOtp = AuthOtp.builder()
                .email(email)
                .otpCode(code)
                .purpose(request.getPurpose() != null ? request.getPurpose() : "LOGIN")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .createdAt(Instant.now())
                .build();
        authOtpRepository.save(authOtp);

        return ResponseEntity.ok(Map.of(
                "status", "OTP_SENT",
                "message", "A 6-digit verification code has been dispatched. For local testing, code: " + code,
                "email", email,
                "code", code
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
                    .fullName(email.split("@")[0])
                    .role("ROLE_TRAVELER")
                    .isActive(true)
                    .createdAt(Instant.now())
                    .build();
            User saved = userRepository.save(newUser);
            userSettingsRepository.save(UserSettings.builder().user(saved).build());
            return saved;
        });

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Mark OTP as used if found
        authOtpRepository.findTopByEmailIgnoreCaseAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                email, otp, Instant.now()
        ).ifPresent(record -> {
            record.setUsed(true);
            authOtpRepository.save(record);
        });

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenService.getExpirationSeconds(),
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "nationality", user.getNationality() != null ? user.getNationality() : "",
                        "passportNumber", user.getPassportNumber() != null ? user.getPassportNumber() : "",
                        "role", user.getRole()
                )
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody RequestOtpDto request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot-password for email: {}", email);

        String code = otpService.generateOtp(email);

        AuthOtp authOtp = AuthOtp.builder()
                .email(email)
                .otpCode(code)
                .purpose("PASSWORD_RESET")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .createdAt(Instant.now())
                .build();
        authOtpRepository.save(authOtp);

        return ResponseEntity.ok(Map.of(
                "status", "OTP_SENT",
                "message", "Password reset code dispatched to email. For local testing, code: " + code,
                "email", email,
                "code", code
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        boolean isValid = otpService.verifyOtp(email, otp);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired verification code."
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "User account not found."
            ));
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "PASSWORD_RESET_SUCCESS",
                "message", "Password has been successfully updated. You may now log in."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated."));
        }

        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .map(u -> {
                    UserSettings s = userSettingsRepository.findById(u.getId()).orElse(null);
                    return ResponseEntity.ok(Map.of(
                            "id", u.getId().toString(),
                            "email", u.getEmail(),
                            "fullName", u.getFullName() != null ? u.getFullName() : "",
                            "nationality", u.getNationality() != null ? u.getNationality() : "",
                            "passportNumber", u.getPassportNumber() != null ? u.getPassportNumber() : "",
                            "role", u.getRole(),
                            "createdAt", u.getCreatedAt().toString(),
                            "settings", s != null ? Map.of(
                                    "emailNotificationsEnabled", s.getEmailNotificationsEnabled(),
                                    "arrivalRemindersEnabled", s.getArrivalRemindersEnabled(),
                                    "advisoryAlertsEnabled", s.getAdvisoryAlertsEnabled(),
                                    "autoResyncEnabled", s.getAutoResyncEnabled(),
                                    "languagePreference", s.getLanguagePreference()
                            ) : Map.of()
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
