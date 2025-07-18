package com.zametech.personalhub.presentation.dto.response;

import java.util.List;

public record ProductivityStatsResponse(
        List<DailyCount> dailyTodoCompletions,
        List<DailyCount> dailyEventCounts,
        List<DailyCount> dailyNoteCreations,
        double weeklyProductivityScore
) {
}