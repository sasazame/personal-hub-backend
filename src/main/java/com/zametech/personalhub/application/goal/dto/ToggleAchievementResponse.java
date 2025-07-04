package com.zametech.personalhub.application.goal.dto;

public record ToggleAchievementResponse(
    String goalId,
    String periodType,
    String periodDate,
    boolean achieved,
    String achievementId
) {}