package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.TestcontainersConfiguration;
import com.zametech.personalhub.domain.model.OAuthApplication;
import com.zametech.personalhub.domain.repository.OAuthApplicationRepository;
import com.zametech.personalhub.infrastructure.security.JwtService;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import com.zametech.personalhub.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class OidcAuthorizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private OAuthApplicationRepository oAuthApplicationRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity testUser;
    private String validToken;
    private OAuthApplication testApplication;

    @BeforeEach
    void setUp() {
        // Create a test OAuth application
        testApplication = OAuthApplication.builder()
                .id(UUID.randomUUID())
                .clientId("test-client")
                .clientSecretHash("$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00j1xOhtEImBq") // bcrypt hash of "test-secret"
                .redirectUris(Arrays.asList("http://localhost:8080/callback", "http://localhost:3000/callback"))
                .scopes(Arrays.asList("openid", "profile", "email"))
                .applicationType("web")
                .grantTypes(Arrays.asList("authorization_code", "refresh_token"))
                .responseTypes(Arrays.asList("code"))
                .tokenEndpointAuthMethod("client_secret_basic")
                .applicationName("Test Application")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testApplication = oAuthApplicationRepository.save(testApplication);

        // Create a test user
        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00j1xOhtEImBq"); // "secret123"
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Generate auth token using JwtService
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities(new ArrayList<>())
                .build();
        validToken = jwtService.generateToken(userDetails);
    }

    @Test
    void authorize_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile email")
                .param("state", "test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login?redirect_uri=*"));
    }

    @Test
    void authorize_WithAuthentication_ShouldReturnAuthorizationCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile email")
                .param("state", "test-state"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).isNotNull();
        assertThat(redirectUrl).startsWith("http://localhost:8080/callback");
        assertThat(redirectUrl).contains("code=");
        assertThat(redirectUrl).contains("state=test-state");
    }

    @Test
    void authorize_WithMissingRequiredParameters_ShouldReturnError() throws Exception {
        // When redirect_uri is missing, it should return 500 error
        mockMvc.perform(get("/auth/authorize")
                .param("client_id", "test-client")
                .param("response_type", "code"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void authorize_WithPKCE_ShouldAcceptCodeChallenge() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile email")
                .param("state", "test-state")
                .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                .param("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).contains("code=");
    }

    @Test
    void authorize_WithAllOptionalParameters_ShouldHandleCorrectly() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile email")
                .param("state", "test-state")
                .param("nonce", "test-nonce")
                .param("prompt", "login")
                .param("display", "page")
                .param("max_age", "3600")
                .param("ui_locales", "en-US")
                .param("acr_values", "urn:mace:incommon:iap:silver"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectUrl).contains("code=");
        assertThat(redirectUrl).contains("state=test-state");
    }

    @Test
    void authorizePost_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "responseType": "code",
                        "clientId": "test-client",
                        "redirectUri": "http://localhost:8080/callback",
                        "scope": "openid profile email",
                        "state": "test-state"
                    }
                    """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Authentication required"));
    }

    @Test
    void authorizePost_WithValidToken_ShouldReturnAuthorizationCode() throws Exception {
        mockMvc.perform(post("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "responseType": "code",
                        "clientId": "test-client",
                        "redirectUri": "http://localhost:8080/callback",
                        "scope": "openid profile email",
                        "state": "test-state"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.state").value("test-state"));
    }

    @Test
    void authorizePost_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clientId": "test-client"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void authorize_WithInvalidResponseType_ShouldReturnError() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/authorize")
                .header("Authorization", "Bearer " + validToken)
                .param("response_type", "token") // implicit flow not supported
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile email"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        
        String redirectedUrl = result.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).contains("error=server_error");
        assertThat(redirectedUrl).contains("error_description=Only+%27code%27+response+type+is+supported");
    }

    @Test
    void authorize_WithServerError_ShouldRedirectWithError() throws Exception {
        // Test with a very long state parameter that might cause issues
        String longState = "x".repeat(10000);
        
        mockMvc.perform(get("/auth/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("state", longState))
                .andExpect(status().is3xxRedirection());
    }
}