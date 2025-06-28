package com.zametech.todoapp.application.goal.dto;

public record ToggleAchievementResponse(
    String goalId,
    String periodType,
    String periodDate,
    boolean achieved,
    String achievementId
) {}