package com.visasetu.domain.entity;

import com.visasetu.domain.converter.StringListJsonConverter;
import com.visasetu.domain.enums.RouteType;
import com.visasetu.domain.enums.VisaPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "visa_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 3)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VisaPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false, length = 50)
    private RouteType routeType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "processing_days_min", nullable = false)
    private Integer processingDaysMin;

    @Column(name = "processing_days_max", nullable = false)
    private Integer processingDaysMax;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays;

    @Column(name = "max_stay_days", nullable = false)
    private Integer maxStayDays;

    @Column(nullable = false, length = 20)
    private String entries;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "allowed_ports", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private List<String> allowedPorts = new ArrayList<>();

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "documents", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private List<String> documents = new ArrayList<>();

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "last_verified_at", nullable = false)
    @Builder.Default
    private Instant lastVerifiedAt = Instant.now();

    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_recommended", nullable = false)
    @Builder.Default
    private Boolean isRecommended = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (lastVerifiedAt == null) lastVerifiedAt = Instant.now();
        if (effectiveFrom == null) effectiveFrom = LocalDate.now();
        if (isRecommended == null) isRecommended = false;
        if (currency == null) currency = "USD";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
