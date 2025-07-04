package com.zametech.personalhub.presentation.dto.response;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String location,
        boolean allDay,
        Integer reminderMinutes,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}