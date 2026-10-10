package com.visasetu.domain.entity;

import com.visasetu.domain.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "visa_category_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaCategoryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "sub_category_name", nullable = false)
    private String subCategoryName;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "purpose_code", nullable = false, length = 50)
    private String purposeCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "who_is_it_for", columnDefinition = "TEXT", nullable = false)
    private String whoIsItFor;

    @Column(name = "validity_text", nullable = false)
    private String validityText;

    @Column(name = "validity_days", nullable = false)
    private Integer validityDays;

    @Column(nullable = false, length = 50)
    private String entries;

    @Column(name = "max_stay_text", nullable = false)
    private String maxStayText;

    @Column(name = "max_stay_days", nullable = false)
    private Integer maxStayDays;

    @Column(name = "fee_band_summary", nullable = false)
    private String feeBandSummary;

    @Column(name = "processing_days_min", nullable = false)
    private Integer processingDaysMin;

    @Column(name = "processing_days_max", nullable = false)
    private Integer processingDaysMax;

    @Column(name = "is_extendable", nullable = false)
    @Builder.Default
    private Boolean isExtendable = false;

    @Column(name = "is_convertible", nullable = false)
    @Builder.Default
    private Boolean isConvertible = false;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "required_documents", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private List<String> requiredDocuments = new ArrayList<>();

    @Column(name = "entry_point_limits", columnDefinition = "TEXT")
    private String entryPointLimits;

    @Column(name = "official_source_url", nullable = false, length = 500)
    private String officialSourceUrl;

    @Column(name = "last_verified_at", nullable = false)
    @Builder.Default
    private Instant lastVerifiedAt = Instant.now();
}
