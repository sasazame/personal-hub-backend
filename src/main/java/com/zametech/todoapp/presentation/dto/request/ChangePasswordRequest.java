package com.zametech.todoapp.presentation.dto.request;

import com.zametech.todoapp.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        
        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword
) {
}