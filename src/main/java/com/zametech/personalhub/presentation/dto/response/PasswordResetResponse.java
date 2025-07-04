package com.zametech.personalhub.presentation.dto.response;

public record PasswordResetResponse(
        String message,
        boolean success
) {}