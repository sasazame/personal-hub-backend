package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMomentRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 5000, message = "Content must not exceed 5000 characters")
        String content,
        
        @Size(max = 10, message = "Cannot have more than 10 tags")
        List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags
) {
}