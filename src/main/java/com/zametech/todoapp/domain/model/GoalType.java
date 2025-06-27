package com.zametech.todoapp.domain.model;

public enum GoalType {
    ANNUAL("Annual"),
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    DAILY("Daily");

    private final String displayName;

    GoalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}