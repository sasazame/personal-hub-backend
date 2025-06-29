package com.zametech.todoapp.presentation.dto.response;

public record PasswordResetResponse(
        String message,
        boolean success
) {}