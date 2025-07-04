package com.zametech.personalhub.presentation.dto.response;

public record GoogleSyncSettingsResponse(
        boolean enabled,
        String calendarId,
        String syncDirection,
        boolean autoSync,
        int syncInterval
) {}