package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.OidcDiscoveryService;
import com.zametech.todoapp.presentation.dto.oidc.OidcDiscoveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OidcDiscoveryController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class OidcDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OidcDiscoveryService oidcDiscoveryService;

    private OidcDiscoveryResponse discoveryResponse;

    @BeforeEach
    void setUp() {
        discoveryResponse = OidcDiscoveryResponse.builder()
            .issuer("http://localhost:8080")
            .authorizationEndpoint("http://localhost:8080/auth/authorize")
            .tokenEndpoint("http://localhost:8080/auth/token")
            .userinfoEndpoint("http://localhost:8080/api/v1/oauth2/userinfo")
            .jwksUri("http://localhost:8080/auth/jwks")
            .scopesSupported(List.of("openid", "profile", "email"))
            .responseTypesSupported(List.of("code"))
            .grantTypesSupported(List.of("authorization_code", "refresh_token"))
            .subjectTypesSupported(List.of("public"))
            .idTokenSigningAlgValuesSupported(List.of("RS256"))
            .tokenEndpointAuthMethodsSupported(List.of("client_secret_basic", "client_secret_post"))
            .claimsSupported(List.of("sub", "name", "email", "email_verified", "preferred_username"))
            .codeChallengeMethodsSupported(List.of("S256", "plain"))
            .build();
    }

    @Test
    void getDiscoveryDocument_ShouldReturnCompleteDocument() throws Exception {
        when(oidcDiscoveryService.getDiscoveryDocument()).thenReturn(discoveryResponse);

        mockMvc.perform(get("/.well-known/openid-configuration"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
            .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8080/auth/authorize"))
            .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8080/auth/token"))
            .andExpect(jsonPath("$.userinfo_endpoint").value("http://localhost:8080/api/v1/oauth2/userinfo"))
            .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/auth/jwks"))
            .andExpect(jsonPath("$.scopes_supported[0]").value("openid"))
            .andExpect(jsonPath("$.response_types_supported[0]").value("code"))
            .andExpect(jsonPath("$.grant_types_supported[0]").value("authorization_code"))
            .andExpect(jsonPath("$.subject_types_supported[0]").value("public"))
            .andExpect(jsonPath("$.id_token_signing_alg_values_supported[0]").value("RS256"))
            .andExpect(jsonPath("$.token_endpoint_auth_methods_supported[0]").value("client_secret_basic"))
            .andExpect(jsonPath("$.claims_supported[0]").value("sub"))
            .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
    }
}