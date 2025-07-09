package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateMomentRequest(
        @NotBlank(message = "Content is required")
        String content,
        
        List<String> tags
) {
}