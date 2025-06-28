package com.zametech.todoapp.application.goal.dto;

import java.time.LocalDate;

public record ToggleAchievementRequest(
        LocalDate date
) {}