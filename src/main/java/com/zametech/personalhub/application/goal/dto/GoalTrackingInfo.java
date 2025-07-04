package com.zametech.personalhub.application.goal.dto;

public record GoalTrackingInfo(
    int totalDays,
    int achievedDays,
    double achievementRate,
    int currentStreak,
    int longestStreak,
    String todayStatus,
    String currentPeriodStatus,
    boolean currentPeriodAchieved
) {}