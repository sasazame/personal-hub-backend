package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        String title,
        
        String description,
        
        @NotNull(message = "Start date and time is required")
        LocalDateTime startDateTime,
        
        @NotNull(message = "End date and time is required")
        LocalDateTime endDateTime,
        
        String location,
        
        boolean allDay,
        
        Integer reminderMinutes,
        
        String color
) {
}