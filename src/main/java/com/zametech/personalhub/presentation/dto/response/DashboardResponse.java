package com.zametech.personalhub.presentation.dto.response;

public record DashboardResponse(
        TodoStatsResponse todoStats,
        EventStatsResponse eventStats,
        NoteStatsResponse noteStats,
        ProductivityStatsResponse productivityStats
) {
}