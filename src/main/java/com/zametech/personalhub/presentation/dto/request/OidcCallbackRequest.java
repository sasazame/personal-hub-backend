package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * OIDC/OAuthコールバックリクエスト
 */
public record OidcCallbackRequest(
    @NotBlank(message = "Authorization code is required")
    String code,
    
    @NotBlank(message = "State parameter is required")
    String state,
    
    String error,
    
    String errorDescription
) {}