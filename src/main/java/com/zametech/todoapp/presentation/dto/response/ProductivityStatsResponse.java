package com.zametech.todoapp.presentation.dto.response;

import java.time.LocalDate;
import java.util.Map;

public record ProductivityStatsResponse(
        Map<LocalDate, Long> dailyTodoCompletions,
        Map<LocalDate, Long> dailyEventCounts,
        Map<LocalDate, Long> dailyNoteCreations,
        double weeklyProductivityScore
) {
}