package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notice_id", nullable = false, unique = true, length = 50)
    private String noticeId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(nullable = false, length = 50)
    private String category; // POLICY, ENTRY_POINTS, FEES, MAINTENANCE, SYSTEM

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String severity = "NORMAL"; // CRITICAL, HIGH, NORMAL, LOW

    @Column(name = "published_at", nullable = false)
    @Builder.Default
    private Instant publishedAt = Instant.now();

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "is_pinned_home", nullable = false)
    @Builder.Default
    private Boolean isPinnedHome = false;
}
