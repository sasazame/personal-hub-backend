package com.zametech.personalhub.application.goal.dto;

import java.time.LocalDate;

public record ToggleAchievementRequest(
        LocalDate date
) {}