package com.zametech.todoapp.application.goal.dto;

import com.zametech.todoapp.domain.model.GoalStatus;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.model.MetricType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalWithTrackingResponse(
    Long id,
    String title,
    String description,
    GoalType goalType,
    MetricType metricType,
    Double targetValue,
    Double currentValue,
    String unit,
    LocalDate startDate,
    LocalDate endDate,
    GoalStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    GoalTrackingInfo trackingInfo
) {}