package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.OidcAuthorizationService;
import com.zametech.personalhub.application.service.UserContextService;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.presentation.dto.oidc.AuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OidcAuthorizationController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class OidcAuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OidcAuthorizationService authorizationService;

    @MockBean
    private UserContextService userContextService;

    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_REDIRECT_URI = "http://localhost:3000/callback";
    private static final String TEST_AUTH_CODE = "test-auth-code";
    private static final String TEST_STATE = "test-state";

    @BeforeEach
    void setUp() {
        when(authorizationService.generateAuthorizationCode(any(AuthorizationRequest.class), any(User.class)))
            .thenReturn(TEST_AUTH_CODE);
        
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        when(userContextService.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithValidRequest_ShouldRedirectWithCode() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI)
                .param("state", TEST_STATE)
                .param("scope", "openid profile")
                .param("nonce", "test-nonce")
                .principal(principal))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=*&state=" + TEST_STATE));
    }

    @Test
    void authorize_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/login?redirect_uri=*"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithOptionalParameters_ShouldHandleCorrectly() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI)
                .param("prompt", "login")
                .param("display", "page")
                .param("max_age", "3600")
                .param("ui_locales", "en-US")
                .param("id_token_hint", "test-token-hint")
                .param("login_hint", "user@example.com")
                .param("acr_values", "urn:mace:incommon:iap:silver")
                .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .param("code_challenge_method", "S256")
                .principal(principal))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=*"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithException_ShouldRedirectWithError() throws Exception {
        when(authorizationService.generateAuthorizationCode(any(AuthorizationRequest.class), any(User.class)))
            .thenThrow(new RuntimeException("Test error"));

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI)
                .param("state", TEST_STATE)
                .principal(principal))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?error=server_error&error_description=*&state=" + TEST_STATE));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorizePost_WithValidRequest_ShouldReturnCode() throws Exception {
        String requestBody = """
            {
                "responseType": "code",
                "clientId": "%s",
                "redirectUri": "%s",
                "state": "%s",
                "scope": "openid profile"
            }
            """.formatted(TEST_CLIENT_ID, TEST_REDIRECT_URI, TEST_STATE);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(TEST_AUTH_CODE))
            .andExpect(jsonPath("$.state").value(TEST_STATE));
    }

    @Test
    void authorizePost_WithoutAuthentication_ShouldReturn401() throws Exception {
        String requestBody = """
            {
                "responseType": "code",
                "clientId": "%s",
                "redirectUri": "%s"
            }
            """.formatted(TEST_CLIENT_ID, TEST_REDIRECT_URI);

        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string("Authentication required"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorizePost_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        when(authorizationService.generateAuthorizationCode(any(AuthorizationRequest.class), any(User.class)))
            .thenThrow(new IllegalArgumentException("Invalid response type"));

        String requestBody = """
            {
                "responseType": "invalid",
                "clientId": "%s",
                "redirectUri": "%s"
            }
            """.formatted(TEST_CLIENT_ID, TEST_REDIRECT_URI);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .principal(principal))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorizePost_WithException_ShouldReturnError() throws Exception {
        when(authorizationService.generateAuthorizationCode(any(AuthorizationRequest.class), any(User.class)))
            .thenThrow(new RuntimeException("Test error"));

        String requestBody = """
            {
                "responseType": "code",
                "clientId": "%s",
                "redirectUri": "%s"
            }
            """.formatted(TEST_CLIENT_ID, TEST_REDIRECT_URI);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .principal(principal))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Test error"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorizePost_WithNullState_ShouldReturnEmptyState() throws Exception {
        String requestBody = """
            {
                "responseType": "code",
                "clientId": "%s",
                "redirectUri": "%s"
            }
            """.formatted(TEST_CLIENT_ID, TEST_REDIRECT_URI);

        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(TEST_AUTH_CODE))
            .andExpect(jsonPath("$.state").value(""));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithRedirectUriContainingQueryParams_ShouldAppendCorrectly() throws Exception {
        String redirectUriWithParams = "http://localhost:3000/callback?existing=param";
        
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", redirectUriWithParams)
                .param("state", TEST_STATE)
                .principal(principal))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(redirectUriWithParams + "&code=*&state=" + TEST_STATE));
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithoutState_ShouldRedirectWithoutState() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI)
                .principal(principal))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=*"))
            .andExpect(result -> {
                String redirectedUrl = result.getResponse().getRedirectedUrl();
                assert redirectedUrl != null && !redirectedUrl.contains("state=");
            });
    }

    @Test
    @WithMockUser(username = "testuser")
    void authorize_WithPKCEParameters_ShouldProcessCorrectly() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", TEST_CLIENT_ID)
                .param("redirect_uri", TEST_REDIRECT_URI)
                .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .param("code_challenge_method", "S256")
                .principal(principal))
            .andExpect(status().is3xxRedirection());
    }
}