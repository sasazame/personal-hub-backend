package com.zametech.todoapp.presentation.dto.response;

public record GoogleSyncSettingsResponse(
        boolean enabled,
        String calendarId,
        String syncDirection,
        boolean autoSync,
        int syncInterval
) {}