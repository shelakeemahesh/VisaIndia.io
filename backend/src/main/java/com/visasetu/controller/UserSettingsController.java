package com.visasetu.controller;

import com.visasetu.domain.entity.User;
import com.visasetu.domain.entity.UserSettings;
import com.visasetu.repository.UserRepository;
import com.visasetu.repository.UserSettingsRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/settings")
@RequiredArgsConstructor
@Slf4j
public class UserSettingsController {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Data
    public static class UpdateSettingsDto {
        private Boolean emailNotificationsEnabled;
        private Boolean arrivalRemindersEnabled;
        private Boolean advisoryAlertsEnabled;
        private Boolean autoResyncEnabled;
        private String languagePreference;
    }

    @GetMapping
    public ResponseEntity<?> getSettings(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Return default settings for guest users
            return ResponseEntity.ok(Map.of(
                    "emailNotificationsEnabled", true,
                    "arrivalRemindersEnabled", true,
                    "advisoryAlertsEnabled", true,
                    "autoResyncEnabled", true,
                    "languagePreference", "en"
            ));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "emailNotificationsEnabled", true,
                    "arrivalRemindersEnabled", true,
                    "advisoryAlertsEnabled", true,
                    "autoResyncEnabled", true,
                    "languagePreference", "en"
            ));
        }

        UserSettings settings = userSettingsRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserSettings s = UserSettings.builder().user(user).build();
                    return userSettingsRepository.save(s);
                });

        return ResponseEntity.ok(Map.of(
                "emailNotificationsEnabled", settings.getEmailNotificationsEnabled(),
                "arrivalRemindersEnabled", settings.getArrivalRemindersEnabled(),
                "advisoryAlertsEnabled", settings.getAdvisoryAlertsEnabled(),
                "autoResyncEnabled", settings.getAutoResyncEnabled(),
                "languagePreference", settings.getLanguagePreference()
        ));
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody UpdateSettingsDto dto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok(Map.of(
                    "status", "UPDATED_LOCAL",
                    "message", "Settings saved for local session."
            ));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "UPDATED_LOCAL",
                    "message", "Settings saved for local session."
            ));
        }

        UserSettings settings = userSettingsRepository.findById(user.getId())
                .orElseGet(() -> UserSettings.builder().user(user).build());

        if (dto.getEmailNotificationsEnabled() != null) {
            settings.setEmailNotificationsEnabled(dto.getEmailNotificationsEnabled());
        }
        if (dto.getArrivalRemindersEnabled() != null) {
            settings.setArrivalRemindersEnabled(dto.getArrivalRemindersEnabled());
        }
        if (dto.getAdvisoryAlertsEnabled() != null) {
            settings.setAdvisoryAlertsEnabled(dto.getAdvisoryAlertsEnabled());
        }
        if (dto.getAutoResyncEnabled() != null) {
            settings.setAutoResyncEnabled(dto.getAutoResyncEnabled());
        }
        if (dto.getLanguagePreference() != null && !dto.getLanguagePreference().isBlank()) {
            settings.setLanguagePreference(dto.getLanguagePreference());
        }
        settings.setUpdatedAt(Instant.now());

        userSettingsRepository.save(settings);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "User settings updated successfully.",
                "settings", Map.of(
                        "emailNotificationsEnabled", settings.getEmailNotificationsEnabled(),
                        "arrivalRemindersEnabled", settings.getArrivalRemindersEnabled(),
                        "advisoryAlertsEnabled", settings.getAdvisoryAlertsEnabled(),
                        "autoResyncEnabled", settings.getAutoResyncEnabled(),
                        "languagePreference", settings.getLanguagePreference()
                )
        ));
    }
}
