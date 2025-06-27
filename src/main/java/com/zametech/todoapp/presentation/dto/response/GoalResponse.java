package com.zametech.todoapp.presentation.dto.response;

import com.zametech.todoapp.domain.model.GoalStatus;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.model.MetricType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private Long id;
    private String title;
    private String description;
    private GoalType goalType;
    private MetricType metricType;
    private String metricUnit;
    private BigDecimal targetValue;
    private BigDecimal currentValue;
    private BigDecimal progressPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private GoalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}