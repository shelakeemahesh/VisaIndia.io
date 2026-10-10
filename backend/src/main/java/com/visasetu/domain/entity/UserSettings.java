package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "arrival_reminders_enabled", nullable = false)
    @Builder.Default
    private Boolean arrivalRemindersEnabled = true;

    @Column(name = "advisory_alerts_enabled", nullable = false)
    @Builder.Default
    private Boolean advisoryAlertsEnabled = true;

    @Column(name = "auto_resync_enabled", nullable = false)
    @Builder.Default
    private Boolean autoResyncEnabled = true;

    @Column(name = "language_preference", nullable = false, length = 10)
    @Builder.Default
    private String languagePreference = "en";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
