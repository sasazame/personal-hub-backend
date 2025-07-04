package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CalendarSyncSettingsRequest(
        @NotBlank(message = "Google Calendar ID is required")
        String googleCalendarId,
        
        String calendarName,
        
        boolean syncEnabled,
        
        @Pattern(regexp = "BIDIRECTIONAL|TO_GOOGLE|FROM_GOOGLE", 
                message = "Sync direction must be BIDIRECTIONAL, TO_GOOGLE, or FROM_GOOGLE")
        String syncDirection
) {
    public CalendarSyncSettingsRequest {
        if (syncDirection == null) {
            syncDirection = "BIDIRECTIONAL";
        }
    }
}