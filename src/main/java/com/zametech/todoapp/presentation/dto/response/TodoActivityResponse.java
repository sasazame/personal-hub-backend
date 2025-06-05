package com.zametech.todoapp.presentation.dto.response;

import java.time.LocalDate;
import java.util.Map;

public record TodoActivityResponse(
        Map<LocalDate, Long> dailyCompletions,
        Map<LocalDate, Long> dailyCreations,
        Map<String, Long> priorityDistribution,
        Map<String, Long> statusDistribution,
        double averageCompletionTimeInDays
) {
}