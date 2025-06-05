package com.zametech.todoapp.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateNoteRequest(
        @NotBlank(message = "Title is required")
        String title,
        
        String content,
        
        List<String> tags
) {
}