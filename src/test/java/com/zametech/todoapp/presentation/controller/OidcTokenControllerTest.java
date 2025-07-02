package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.OidcTokenService;
import com.zametech.todoapp.presentation.dto.oidc.TokenRequest;
import com.zametech.todoapp.presentation.dto.oidc.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OidcTokenController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class OidcTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OidcTokenService tokenService;

    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        tokenResponse = TokenResponse.builder()
            .accessToken("test-access-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .refreshToken("test-refresh-token")
            .scope("openid profile")
            .idToken("test-id-token")
            .build();
    }

    @Test
    void token_WithAuthorizationCodeGrant_ShouldReturnTokens() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "test-auth-code")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("test-access-token"))
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(3600))
            .andExpect(jsonPath("$.refresh_token").value("test-refresh-token"))
            .andExpect(jsonPath("$.scope").value("openid profile"))
            .andExpect(jsonPath("$.id_token").value("test-id-token"));
    }

    @Test
    void token_WithRefreshTokenGrant_ShouldReturnNewTokens() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", "test-refresh-token")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("test-access-token"));
    }

    @Test
    void token_WithBasicAuth_ShouldExtractClientCredentials() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class))).thenReturn(tokenResponse);

        String credentials = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic " + credentials)
                .param("grant_type", "authorization_code")
                .param("code", "test-auth-code")
                .param("redirect_uri", "http://localhost:3000/callback"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("test-access-token"));
    }

    @Test
    void token_WithPKCE_ShouldIncludeCodeVerifier() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "test-auth-code")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("client_id", "test-client")
                .param("code_verifier", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("test-access-token"));
    }

    @Test
    void token_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid grant type"));

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "invalid_grant")
                .param("code", "test-code"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Invalid grant type"));
    }

    @Test
    void token_WithServerError_ShouldReturn500() throws Exception {
        when(tokenService.processTokenRequest(any(TokenRequest.class)))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "test-code"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("server_error"))
            .andExpect(jsonPath("$.error_description").value("Internal server error"));
    }

    @Test
    void revoke_WithValidToken_ShouldReturn200() throws Exception {
        when(tokenService.revokeToken("test-token", "access_token", "test-client"))
            .thenReturn(true);

        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "test-token")
                .param("token_type_hint", "access_token")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    void revoke_WithTokenNotFound_ShouldStillReturn200() throws Exception {
        when(tokenService.revokeToken("test-token", null, "test-client"))
            .thenReturn(false);

        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "test-token")
                .param("client_id", "test-client"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    void revoke_WithBasicAuth_ShouldExtractClientCredentials() throws Exception {
        when(tokenService.revokeToken("test-token", "refresh_token", "test-client"))
            .thenReturn(true);

        String credentials = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());

        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic " + credentials)
                .param("token", "test-token")
                .param("token_type_hint", "refresh_token"))
            .andExpect(status().isOk());
    }

    @Test
    void revoke_WithMissingToken_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("client_id", "test-client"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void revoke_WithEmptyToken_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "   ")
                .param("client_id", "test-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Missing required parameter: token"));
    }

    @Test
    void revoke_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        when(tokenService.revokeToken(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Invalid client"));

        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "test-token")
                .param("client_id", "invalid-client"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Invalid client"));
    }

    @Test
    void revoke_WithServerError_ShouldReturn500() throws Exception {
        when(tokenService.revokeToken(any(), any(), any()))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "test-token"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("server_error"))
            .andExpect(jsonPath("$.error_description").value("Internal server error"));
    }
}