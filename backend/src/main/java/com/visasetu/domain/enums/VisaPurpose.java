package com.visasetu.domain.enums;

public enum VisaPurpose {
    TOURISM("Tourism & Recreation"),
    BUSINESS("Business & Commerce"),
    MEDICAL("Medical Treatment"),
    CONFERENCE("International Conference"),
    TRANSIT("Transit");

    private final String displayName;

    VisaPurpose(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
