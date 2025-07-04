package com.zametech.personalhub.presentation.dto.oidc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record AuthorizationRequest(
    @NotBlank(message = "response_type is required")
    @Pattern(regexp = "code", message = "Only 'code' response type is supported")
    String responseType,
    
    @NotBlank(message = "client_id is required")
    String clientId,
    
    @NotBlank(message = "redirect_uri is required")
    String redirectUri,
    
    String scope,
    
    String state,
    
    String nonce,
    
    String prompt,
    
    String display,
    
    String maxAge,
    
    String uiLocales,
    
    String idTokenHint,
    
    String loginHint,
    
    String acrValues,
    
    String codeChallenge,
    
    @Pattern(regexp = "plain|S256", message = "code_challenge_method must be 'plain' or 'S256'")
    String codeChallengeMethod
) {
    public static AuthorizationRequest fromQueryParams(
        String responseType,
        String clientId,
        String redirectUri,
        String scope,
        String state,
        String nonce,
        String prompt,
        String display,
        String maxAge,
        String uiLocales,
        String idTokenHint,
        String loginHint,
        String acrValues,
        String codeChallenge,
        String codeChallengeMethod
    ) {
        return AuthorizationRequest.builder()
            .responseType(responseType)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope(scope)
            .state(state)
            .nonce(nonce)
            .prompt(prompt)
            .display(display)
            .maxAge(maxAge)
            .uiLocales(uiLocales)
            .idTokenHint(idTokenHint)
            .loginHint(loginHint)
            .acrValues(acrValues)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .build();
    }
}