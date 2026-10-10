package com.visasetu.dto;

import com.visasetu.domain.enums.VisaPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {
    private String nationality;
    private String nationalityName;
    private VisaPurpose purpose;
    private LocalDate arrivalDate;
    private int routesFound;
    @Builder.Default
    private List<EligibilityRouteDto> routes = new ArrayList<>();
    private String fallbackMessage;
    private String generalDisclaimer;
}
