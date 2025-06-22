package com.zametech.todoapp.presentation.dto.oidc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record TokenRequest(
    @NotBlank(message = "grant_type is required")
    @Pattern(regexp = "authorization_code|refresh_token", message = "Only 'authorization_code' and 'refresh_token' grant types are supported")
    String grantType,
    
    String code,
    
    String redirectUri,
    
    String clientId,
    
    String clientSecret,
    
    String codeVerifier,
    
    String refreshToken,
    
    String scope
) {}