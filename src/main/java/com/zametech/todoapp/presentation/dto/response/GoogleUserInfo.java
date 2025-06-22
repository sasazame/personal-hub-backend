package com.zametech.todoapp.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Google UserInfo APIレスポンス
 */
public record GoogleUserInfo(
    String sub,
    String email,
    @JsonProperty("email_verified")
    Boolean emailVerified,
    String name,
    @JsonProperty("given_name")
    String givenName,
    @JsonProperty("family_name")
    String familyName,
    String picture,
    String locale
) {}