package com.visasetu.domain.enums;

public enum RouteType {
    VISA_FREE("Visa-Free Entry"),
    E_VISA("Electronic Visa (e-Visa)"),
    VISA_ON_ARRIVAL("Visa on Arrival (VoA)"),
    EMBASSY_VISA("Regular Embassy / Consulate Visa");

    private final String displayName;

    RouteType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
