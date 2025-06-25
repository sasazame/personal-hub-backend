package com.zametech.todoapp.presentation.dto.response;

import java.util.List;

public record GoogleSyncStatusResponse(
        String lastSyncTime,
        String nextSyncTime,
        boolean isRunning,
        int syncedEvents,
        List<String> errors
) {}