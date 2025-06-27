package com.zametech.todoapp.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalProgressResponse {
    private Long id;
    private Long goalId;
    private LocalDate date;
    private BigDecimal value;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}