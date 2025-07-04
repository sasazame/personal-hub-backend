package com.zametech.personalhub.application.goal.dto;

import com.zametech.personalhub.domain.model.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank(message = "Title is required")
        String title,
        
        String description,
        
        @NotNull(message = "Goal type is required")
        GoalType goalType,
        
        LocalDate startDate,
        LocalDate endDate
) {}