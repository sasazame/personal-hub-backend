package com.zametech.todoapp.application.goal.dto;

import com.zametech.todoapp.domain.model.GoalType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalResponse(
        Long id,
        String title,
        String description,
        GoalType goalType,
        Boolean isActive,
        LocalDate startDate,
        LocalDate endDate,
        Boolean completed,
        Integer currentStreak,
        Integer longestStreak,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}