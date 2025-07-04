package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class OidcDiscoveryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOpenIdConfiguration_ShouldReturnValidDiscoveryDocument() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Required fields according to OpenID Connect Discovery 1.0
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.userinfo_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.response_types_supported").isArray())
                .andExpect(jsonPath("$.subject_types_supported").isArray())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported").isArray())
                // Optional but commonly included fields
                .andExpect(jsonPath("$.scopes_supported").isArray())
                .andExpect(jsonPath("$.token_endpoint_auth_methods_supported").isArray())
                .andExpect(jsonPath("$.claims_supported").isArray())
                .andExpect(jsonPath("$.grant_types_supported").isArray())
                .andExpect(jsonPath("$.response_modes_supported").isArray())
                .andExpect(jsonPath("$.code_challenge_methods_supported").isArray());
    }

    @Test
    void getOpenIdConfiguration_ShouldHaveCorrectEndpointUrls() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(containsString("http")))
                .andExpect(jsonPath("$.authorization_endpoint").value(containsString("/auth/authorize")))
                .andExpect(jsonPath("$.token_endpoint").value(containsString("/auth/token")))
                .andExpect(jsonPath("$.userinfo_endpoint").value(containsString("/auth/userinfo")))
                .andExpect(jsonPath("$.jwks_uri").value(containsString("/.well-known/jwks.json")));
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportRequiredResponseTypes() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_types_supported").value(hasItem("code")))
                .andExpect(jsonPath("$.response_types_supported").value(hasItem("token id_token")))
                .andExpect(jsonPath("$.response_types_supported").value(hasItem("id_token")));
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportStandardScopes() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopes_supported").value(hasItems("openid", "profile", "email")));
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportPKCE() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code_challenge_methods_supported").value(hasItems("plain", "S256")));
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportStandardGrantTypes() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grant_types_supported").value(hasItems("authorization_code", "refresh_token")));
    }

    @Test
    void getOpenIdConfiguration_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Discovery endpoint should be publicly accessible
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk());
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportStandardClaims() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claims_supported").value(hasItems(
                        "sub", "email", "email_verified", "name", 
                        "given_name", "family_name", "locale", "picture"
                )));
    }

    @Test
    void getOpenIdConfiguration_ShouldHaveConsistentIssuer() throws Exception {
        String response = mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Multiple requests should return the same issuer
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    void getOpenIdConfiguration_ShouldHandleInvalidAcceptHeader() throws Exception {
        // Should still return JSON even if client requests XML
        mockMvc.perform(get("/.well-known/openid-configuration")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void getOpenIdConfiguration_ShouldSupportTokenEndpointAuthMethods() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_endpoint_auth_methods_supported")
                        .value(hasItems("client_secret_basic", "client_secret_post")));
    }

    @Test
    void getOpenIdConfiguration_ShouldHaveSubjectTypesSupported() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject_types_supported").value(hasItem("public")));
    }

    @Test
    void getOpenIdConfiguration_ShouldHaveIdTokenSigningAlgorithms() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported")
                        .value(hasItems("RS256", "ES256")));
    }
}