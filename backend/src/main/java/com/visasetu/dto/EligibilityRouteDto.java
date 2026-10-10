package com.visasetu.dto;

import com.visasetu.domain.enums.RouteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRouteDto {
    private UUID id;
    private RouteType routeType;
    private String routeTypeName;
    private String title;
    private String description;
    private BigDecimal fee;
    private String currency;
    private Integer processingDaysMin;
    private Integer processingDaysMax;
    private String processingTimeText;
    private Integer validityDays;
    private Integer maxStayDays;
    private String entries;
    private List<String> allowedPorts;
    private List<String> documents;
    private String sourceUrl;
    private Instant lastVerifiedAt;
    private LocalDate applyByDate;
    private Boolean isRecommended;
    private String dataDisclaimer;
}
