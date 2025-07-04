package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record GoogleSyncSettingsRequest(
        boolean enabled,
        
        String calendarId,
        
        @Pattern(regexp = "TO_GOOGLE|FROM_GOOGLE|BIDIRECTIONAL", 
                message = "Sync direction must be TO_GOOGLE, FROM_GOOGLE, or BIDIRECTIONAL")
        String syncDirection,
        
        boolean autoSync,
        
        @Min(value = 1, message = "Sync interval must be at least 1 minute")
        int syncInterval
) {
    public GoogleSyncSettingsRequest {
        if (syncDirection == null) {
            syncDirection = "BIDIRECTIONAL";
        }
        if (syncInterval <= 0) {
            syncInterval = 30;
        }
    }
}