package com.zametech.todoapp.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String metricUnit;
    
    @NotNull(message = "Target value is required")
    @Positive(message = "Target value must be positive")
    private BigDecimal targetValue;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
}