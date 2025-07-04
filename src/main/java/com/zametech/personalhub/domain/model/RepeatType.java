package com.zametech.personalhub.domain.model;

/**
 * Enumeration for TODO repeat types
 */
public enum RepeatType {
    DAILY,      // Repeat daily
    WEEKLY,     // Repeat weekly on specified days
    MONTHLY,    // Repeat monthly on the same day
    YEARLY,     // Repeat yearly on the same date
    ONCE        // One-time occurrence (specific date)
}