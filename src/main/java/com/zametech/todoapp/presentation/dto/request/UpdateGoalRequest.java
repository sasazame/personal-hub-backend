package com.zametech.todoapp.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private Boolean isActive;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
}