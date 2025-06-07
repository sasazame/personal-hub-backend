package com.zametech.todoapp.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarSyncStatusResponse(
        boolean isConnected,
        LocalDateTime lastSyncAt,
        String syncStatus,
        List<CalendarSyncSettingsResponse> connectedCalendars,
        SyncStatistics syncStatistics
) {
    public record SyncStatistics(
            int totalEvents,
            int syncedEvents,
            int pendingEvents,
            int errorEvents,
            LocalDateTime lastSuccessfulSync
    ) {}
}