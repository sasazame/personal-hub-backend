package com.zametech.personalhub.presentation.dto.oidc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    @JsonProperty("expires_in")
    Long expiresIn,
    
    @JsonProperty("refresh_token")
    String refreshToken,
    
    @JsonProperty("scope")
    String scope,
    
    @JsonProperty("id_token")
    String idToken
) {}