package com.zametech.personalhub.presentation.dto.response;

import java.time.LocalDateTime;

public record CalendarSyncSettingsResponse(
        Long id,
        String googleCalendarId,
        String calendarName,
        boolean syncEnabled,
        LocalDateTime lastSyncAt,
        String syncDirection,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}