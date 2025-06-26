package com.zametech.todoapp.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWeekStartDayRequest {
    @NotNull(message = "Week start day is required")
    @Min(value = 0, message = "Week start day must be between 0 and 6")
    @Max(value = 6, message = "Week start day must be between 0 and 6")
    private Integer weekStartDay;
}