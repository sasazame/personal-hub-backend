package com.zametech.todoapp.presentation.dto.response;

import java.time.LocalDate;

public record DailyCount(
        LocalDate date,
        Integer count
) {
}