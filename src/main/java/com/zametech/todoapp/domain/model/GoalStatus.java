package com.zametech.todoapp.domain.model;

public enum GoalStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    ARCHIVED("Archived"),
    PAUSED("Paused");

    private final String displayName;

    GoalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}