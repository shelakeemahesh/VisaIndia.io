package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID draftId;

    @Column(name = "reference_id", nullable = false, unique = true, length = 50)
    private String referenceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "applicant_email", nullable = false, length = 150)
    private String applicantEmail;

    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private Integer currentStep = 1;

    @Column(length = 100)
    private String nationality;

    @Column(name = "category_name", length = 150)
    private String categoryName;

    @Column(name = "shared_profile_json", columnDefinition = "TEXT")
    private String sharedProfileJson;

    @Column(name = "modules_json", columnDefinition = "TEXT")
    private String modulesJson;

    @Column(name = "is_submitted", nullable = false)
    @Builder.Default
    private Boolean isSubmitted = false;

    @Column(name = "trip_id")
    private UUID tripId;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
