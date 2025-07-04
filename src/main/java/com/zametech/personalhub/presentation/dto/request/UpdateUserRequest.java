package com.zametech.personalhub.presentation.dto.request;

import com.zametech.personalhub.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Current password is required")
        String currentPassword,
        
        @StrongPassword
        String newPassword
) {
}