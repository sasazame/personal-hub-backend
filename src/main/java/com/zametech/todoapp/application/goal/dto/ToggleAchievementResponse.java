package com.zametech.todoapp.application.goal.dto;

public record ToggleAchievementResponse(
    Long goalId,
    String periodType,
    String periodDate,
    boolean achieved,
    Long progressId
) {}