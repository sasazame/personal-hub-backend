package com.zametech.todoapp.domain.model;

public enum MetricType {
    COUNT("Count"),
    NUMERIC("Numeric"),
    PERCENTAGE("Percentage"),
    TIME("Time");

    private final String displayName;

    MetricType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}