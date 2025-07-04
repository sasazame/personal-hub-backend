package com.zametech.personalhub.presentation.dto.request;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String location,
        Boolean allDay,
        Integer reminderMinutes,
        String color
) {
}