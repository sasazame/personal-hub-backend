package com.zametech.personalhub.presentation.dto.response;

public record EventStatsResponse(
        long totalEvents,
        long upcomingEvents,
        long pastEvents,
        long todayEvents
) {
}