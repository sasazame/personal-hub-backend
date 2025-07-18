package com.zametech.personalhub.application.goal.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGoalRequest(
        @NotBlank(message = "Title is required")
        String title,
        
        String description,
        
        Boolean isActive
) {}