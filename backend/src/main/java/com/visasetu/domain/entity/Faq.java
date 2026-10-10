package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String topic; // BEFORE_APPLYING, APPLICATION, PAYMENT, AFTER_APPROVAL, ARRIVAL, TECHNICAL

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "order_num", nullable = false)
    @Builder.Default
    private Integer orderNum = 0;

    @Column(name = "official_source_url", nullable = false, length = 500)
    private String officialSourceUrl;

    @Column(name = "last_verified_at", nullable = false)
    @Builder.Default
    private Instant lastVerifiedAt = Instant.now();
}
