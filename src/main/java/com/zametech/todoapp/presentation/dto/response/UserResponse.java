package com.zametech.todoapp.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Integer weekStartDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}