package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalMilestone {
    private Long id;
    private Long goalId;
    private String title;
    private BigDecimal targetValue;
    private Boolean achieved;
    private LocalDate achievedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}