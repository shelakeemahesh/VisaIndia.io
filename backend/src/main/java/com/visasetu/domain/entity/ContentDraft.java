package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_draft")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(name = "category_slug", length = 100)
    private String categorySlug;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "source_type", nullable = false, length = 50)
    @Builder.Default
    private String sourceType = "OFFICIAL"; // OFFICIAL or SECONDARY

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_json", columnDefinition = "TEXT", nullable = false)
    private String contentJson;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private Instant fetchedAt = Instant.now();

    @Column(name = "last_verified_at", nullable = false)
    @Builder.Default
    private Instant lastVerifiedAt = Instant.now();

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;
}
