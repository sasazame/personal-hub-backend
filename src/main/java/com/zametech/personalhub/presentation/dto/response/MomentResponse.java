package com.zametech.personalhub.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MomentResponse(
        Long id,
        String content,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}