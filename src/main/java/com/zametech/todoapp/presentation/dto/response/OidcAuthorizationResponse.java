package com.zametech.todoapp.presentation.dto.response;

/**
 * OIDC認証開始レスポンス
 */
public record OidcAuthorizationResponse(
    String authorizationUrl,
    String state,
    String provider
) {}