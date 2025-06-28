package com.zametech.todoapp.application.goal.dto;

import java.time.LocalDate;
import java.util.List;

public record AchievementHistoryResponse(
        List<AchievementRecord> achievements,
        Integer totalDays,
        Integer achievedDays,
        Double achievementRate
) {
    public record AchievementRecord(
            LocalDate date,
            Boolean achieved
    ) {}
}