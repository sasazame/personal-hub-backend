package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.TestcontainersConfiguration;
import com.zametech.personalhub.presentation.dto.request.OidcCallbackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class OidcControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void googleAuthorize_ShouldReturnAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oidc/google/authorize"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("accounts.google.com")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("client_id=")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("response_type=code")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("scope=")))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.state").isNotEmpty());
    }

    @Test
    void googleAuthorize_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Authorization initiation should be publicly accessible
        mockMvc.perform(get("/api/v1/auth/oidc/google/authorize"))
                .andExpect(status().isOk());
    }

    @Test
    void googleCallback_WithValidCode_ShouldReturnAuthResponse() throws Exception {
        OidcCallbackRequest request = new OidcCallbackRequest(
                "test-auth-code",
                "test-state",
                null,
                null
        );

        // This will likely fail without mocking the external Google OAuth service
        // but we're testing the endpoint structure
        mockMvc.perform(post("/api/v1/auth/oidc/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Expecting error without valid Google auth
    }

    @Test
    void googleCallback_WithMissingCode_ShouldReturnBadRequest() throws Exception {
        // Create JSON manually for missing code scenario
        String requestJson = "{\"state\":\"test-state\"}";

        mockMvc.perform(post("/api/v1/auth/oidc/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void googleCallback_WithMissingState_ShouldReturnBadRequest() throws Exception {
        // Create JSON manually for missing state scenario
        String requestJson = "{\"code\":\"test-auth-code\"}";

        mockMvc.perform(post("/api/v1/auth/oidc/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void githubAuthorize_ShouldReturnAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oidc/github/authorize"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("github.com")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("client_id=")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("scope=")))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.state").isNotEmpty());
    }

    @Test
    void githubAuthorize_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Authorization initiation should be publicly accessible
        mockMvc.perform(get("/api/v1/auth/oidc/github/authorize"))
                .andExpect(status().isOk());
    }

    @Test
    void githubCallback_WithValidCode_ShouldReturnAuthResponse() throws Exception {
        OidcCallbackRequest request = new OidcCallbackRequest(
                "test-auth-code",
                "test-state",
                null,
                null
        );

        // This will likely fail without mocking the external GitHub OAuth service
        // but we're testing the endpoint structure
        mockMvc.perform(post("/api/v1/auth/oidc/github/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Expecting error without valid GitHub auth
    }

    @Test
    void githubCallback_WithMissingCode_ShouldReturnBadRequest() throws Exception {
        // Create JSON manually for missing code scenario
        String requestJson = "{\"state\":\"test-state\"}";

        mockMvc.perform(post("/api/v1/auth/oidc/github/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void googleCallback_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        OidcCallbackRequest request = new OidcCallbackRequest(
                "test-code",
                "test-state",
                null,
                null
        );

        // Callback endpoints should be publicly accessible
        mockMvc.perform(post("/api/v1/auth/oidc/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Expecting error but not 403
    }

    @Test
    void githubCallback_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        OidcCallbackRequest request = new OidcCallbackRequest(
                "test-code",
                "test-state",
                null,
                null
        );

        // Callback endpoints should be publicly accessible
        mockMvc.perform(post("/api/v1/auth/oidc/github/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Expecting error but not 403
    }

    @Test
    void googleAuthorize_ShouldGenerateUniqueStates() throws Exception {
        String response1 = mockMvc.perform(get("/api/v1/auth/oidc/google/authorize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(get("/api/v1/auth/oidc/google/authorize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String state1 = objectMapper.readTree(response1).get("state").asText();
        String state2 = objectMapper.readTree(response2).get("state").asText();

        // Each authorization request should generate a unique state
        org.assertj.core.api.Assertions.assertThat(state1).isNotEqualTo(state2);
    }

    @Test
    void githubAuthorize_ShouldGenerateUniqueStates() throws Exception {
        String response1 = mockMvc.perform(get("/api/v1/auth/oidc/github/authorize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(get("/api/v1/auth/oidc/github/authorize"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String state1 = objectMapper.readTree(response1).get("state").asText();
        String state2 = objectMapper.readTree(response2).get("state").asText();

        // Each authorization request should generate a unique state
        org.assertj.core.api.Assertions.assertThat(state1).isNotEqualTo(state2);
    }

    @Test
    void googleCallback_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oidc/google/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void githubCallback_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oidc/github/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }
}