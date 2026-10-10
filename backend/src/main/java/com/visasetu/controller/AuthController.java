package com.visasetu.controller;

import com.visasetu.domain.entity.User;
import com.visasetu.domain.entity.UserSettings;
import com.visasetu.repository.UserRepository;
import com.visasetu.repository.UserSettingsRepository;
import com.visasetu.security.SecurityUtils;
import com.visasetu.service.JwtTokenService;
import com.visasetu.service.OtpService;
import com.visasetu.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final OtpService otpService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitingService rateLimitingService;

    @Data
    public static class SignupRequestDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        private String fullName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
        private String password;

        private String nationality;
        private String passportNumber;
        private String phoneNumber;
    }

    @Data
    public static class VerifySignupOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        private String otp;
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

        private String purpose; // "LOGIN", "SIGNUP_VERIFY", "PASSWORD_RESET"
    }

    @Data
    public static class VerifyOtpDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        private String otp;

        private String purpose = "LOGIN";
    }

    @Data
    public static class ResetPasswordDto {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
        private String otp;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number")
        private String newPassword;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitingService.tryAcquire("signup:" + clientIp, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many registration attempts. Please wait before retrying."
            ));
        }

        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing signup for email: {}", email);

        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "An account with this email address already exists. Please log in."
                ));
            } else {
                // Re-send verification OTP for unverified existing signup
                otpService.generateOtp(email, "SIGNUP_VERIFY");
                return ResponseEntity.ok(Map.of(
                        "status", "VERIFICATION_REQUIRED",
                        "message", "A verification code has been dispatched to your email address.",
                        "email", email
                ));
            }
        }

        User newUser = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword().trim()))
                .nationality(request.getNationality() != null ? request.getNationality().trim() : null)
                .passportNumber(request.getPassportNumber() != null ? request.getPassportNumber().trim() : null)
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role("ROLE_TRAVELER") // Never allow public signup to assign admin roles
                .isActive(true)
                .emailVerified(false) // Strict email verification required
                .createdAt(Instant.now())
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

        // Dispatch verification OTP
        otpService.generateOtp(email, "SIGNUP_VERIFY");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "VERIFICATION_REQUIRED",
                "message", "Registration initiated. A 6-digit verification code has been dispatched to your email address.",
                "email", email
        ));
    }

    @PostMapping("/verify-signup-otp")
    public ResponseEntity<?> verifySignupOtp(@Valid @RequestBody VerifySignupOtpDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimitingService.tryAcquire("verify-signup:" + email + ":" + clientIp, 5, 300)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many verification attempts. Please wait 5 minutes."
            ));
        }

        boolean isValid = otpService.verifyOtp(email, request.getOtp().trim(), "SIGNUP_VERIFY");
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired verification code."
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User account not found."));
        }

        user.setEmailVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());
        UserSettings settings = userSettingsRepository.findById(user.getId()).orElse(null);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Email verified successfully.",
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", jwtTokenService.getExpirationSeconds(),
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimitingService.tryAcquire("login:" + email + ":" + clientIp, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many failed login attempts. Please wait a minute before retrying."
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        // Security requirement: Never silently create accounts on login for unknown emails
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid email or password."
            ));
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "This account has been deactivated. Please contact support."
            ));
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Your email address is not yet verified. Please verify using the signup verification code.",
                    "code", "EMAIL_NOT_VERIFIED",
                    "email", email
            ));
        }

        // Authentication by OTP if provided
        if (request.getOtp() != null && !request.getOtp().isBlank()) {
            boolean isValidOtp = otpService.verifyOtp(email, request.getOtp().trim(), "LOGIN");
            if (!isValidOtp) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid or expired 6-digit verification code."
                ));
            }
        } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String pwd = request.getPassword().trim();
            // Strict BCrypt verification: no hardcoded bypasses
            if (user.getPasswordHash() == null || !passwordEncoder.matches(pwd, user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid email or password."
                ));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Password or verification code is required."));
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
    public ResponseEntity<?> requestOtp(@Valid @RequestBody RequestOtpDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimitingService.tryAcquire("request-otp:" + email + ":" + clientIp, 5, 300)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many OTP requests. Please wait a few minutes."
            ));
        }

        String purpose = request.getPurpose() != null && !request.getPurpose().isBlank()
                ? request.getPurpose().trim().toUpperCase()
                : "LOGIN";

        // Dispatch OTP if registered or during signup verification
        boolean userExists = userRepository.existsByEmailIgnoreCase(email);
        if (userExists || "SIGNUP_VERIFY".equals(purpose)) {
            otpService.generateOtp(email, purpose);
        }

        // Generic response prevents user enumeration; NEVER expose raw code
        return ResponseEntity.ok(Map.of(
                "status", "OTP_SENT",
                "message", "If this email address is registered, a 6-digit security code has been dispatched.",
                "email", email
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();
        String purpose = request.getPurpose() != null ? request.getPurpose() : "LOGIN";

        if (!rateLimitingService.tryAcquire("verify-otp:" + email + ":" + clientIp, 5, 300)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many verification attempts. Please wait 5 minutes."
            ));
        }

        boolean isValid = otpService.verifyOtp(email, otp, purpose);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid or expired verification code."
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User account not found."));
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());

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
                )
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody RequestOtpDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();

        if (!rateLimitingService.tryAcquire("forgot-password:" + email + ":" + clientIp, 3, 300)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many password recovery requests. Please wait before retrying."
            ));
        }

        log.info("Processing forgot-password for email: {}", email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            otpService.generateOtp(email, "PASSWORD_RESET");
        }

        // Generic response prevents account enumeration; never leaks code
        return ResponseEntity.ok(Map.of(
                "status", "OTP_SENT",
                "message", "If an account exists with this email, password recovery instructions have been dispatched.",
                "email", email
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        if (!rateLimitingService.tryAcquire("reset-password:" + email + ":" + clientIp, 5, 300)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too many password reset attempts. Please wait 5 minutes."
            ));
        }

        boolean isValid = otpService.verifyOtp(email, otp, "PASSWORD_RESET");
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
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "PASSWORD_RESET_SUCCESS",
                "message", "Password has been successfully updated. You may now log in with your new credentials."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Optional<String> currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (currentUserEmail.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated."));
        }

        return userRepository.findByEmailIgnoreCase(currentUserEmail.get())
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
