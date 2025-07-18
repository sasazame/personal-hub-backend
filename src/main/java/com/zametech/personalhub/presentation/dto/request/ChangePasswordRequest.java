package com.zametech.personalhub.presentation.dto.request;

import com.zametech.personalhub.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        
        @NotBlank(message = "New password is required")
        @StrongPassword
        String newPassword
) {
}