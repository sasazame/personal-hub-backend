package com.zametech.todoapp.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordProgressRequest {
    @NotNull(message = "Value is required")
    @PositiveOrZero(message = "Value must be zero or positive")
    private BigDecimal value;
    
    private LocalDate date = LocalDate.now();
    
    private String note;
}