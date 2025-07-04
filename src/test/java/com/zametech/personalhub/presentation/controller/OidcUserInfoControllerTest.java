package com.zametech.personalhub.presentation.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.personalhub.application.service.OidcUserInfoService;
import com.zametech.personalhub.presentation.dto.oidc.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OidcUserInfoController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class OidcUserInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OidcUserInfoService userInfoService;

    private UserInfoResponse userInfoResponse;
    private String validAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        userInfoResponse = UserInfoResponse.builder()
            .sub("user123")
            .name("Test User")
            .givenName("Test")
            .familyName("User")
            .email("test@example.com")
            .emailVerified(true)
            .preferredUsername("testuser")
            .build();

        // Create a valid JWT token for testing
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("user123")
            .issuer("http://localhost:8080")
            .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000))
            .claim("scope", "openid profile email")
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        );

        // Sign with a 256-bit key
        signedJWT.sign(new MACSigner("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"));
        validAccessToken = signedJWT.serialize();
    }

    @Test
    void getUserInfo_WithValidToken_ShouldReturnUserInfo() throws Exception {
        when(userInfoService.getUserInfo(eq(validAccessToken), any(List.class)))
            .thenReturn(userInfoResponse);

        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer " + validAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("user123"))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.given_name").value("Test"))
            .andExpect(jsonPath("$.family_name").value("User"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.email_verified").value(true))
            .andExpect(jsonPath("$.preferred_username").value("testuser"));
    }

    @Test
    void getUserInfo_WithoutAuthorizationHeader_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/userinfo"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").value("Bearer token required"));
    }

    @Test
    void getUserInfo_WithInvalidTokenFormat_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Basic dGVzdDp0ZXN0"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").value("Bearer token required"));
    }

    @Test
    void getUserInfo_WithMalformedToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer invalid-jwt-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").exists());
    }

    @Test
    void getUserInfo_WithAllScopes_ShouldReturnFullUserInfo() throws Exception {
        UserInfoResponse fullUserInfo = UserInfoResponse.builder()
            .sub("user123")
            .name("Test User")
            .givenName("Test")
            .familyName("User")
            .middleName("Middle")
            .nickname("testy")
            .preferredUsername("testuser")
            .profile("https://example.com/profile")
            .picture("https://example.com/picture.jpg")
            .website("https://example.com")
            .email("test@example.com")
            .emailVerified(true)
            .gender("other")
            .birthdate("1990-01-01")
            .zoneinfo("America/New_York")
            .locale("en-US")
            .phoneNumber("+1234567890")
            .phoneNumberVerified(true)
            .address(new UserInfoResponse.AddressInfo(
                "123 Main St, City, State 12345",
                "123 Main St",
                "City",
                "State",
                "12345",
                "US"
            ))
            .updatedAt(1234567890L)
            .build();

        when(userInfoService.getUserInfo(eq(validAccessToken), any(List.class)))
            .thenReturn(fullUserInfo);

        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer " + validAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("user123"))
            .andExpect(jsonPath("$.nickname").value("testy"))
            .andExpect(jsonPath("$.phone_number").value("+1234567890"))
            .andExpect(jsonPath("$.address.street_address").value("123 Main St"))
            .andExpect(jsonPath("$.updated_at").value(1234567890L));
    }

    @Test
    void getUserInfoPost_WithValidToken_ShouldReturnUserInfo() throws Exception {
        when(userInfoService.getUserInfo(eq(validAccessToken), any(List.class)))
            .thenReturn(userInfoResponse);

        mockMvc.perform(post("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer " + validAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("user123"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserInfo_WithServiceException_ShouldReturn401() throws Exception {
        when(userInfoService.getUserInfo(any(), any()))
            .thenThrow(new RuntimeException("Token validation failed"));

        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer " + validAccessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_token"))
            .andExpect(jsonPath("$.error_description").value("Token validation failed"));
    }

    @Test
    void getUserInfo_WithTokenWithoutScope_ShouldHandleEmptyScopes() throws Exception {
        // Create a token without scope claim
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("user123")
            .issuer("http://localhost:8080")
            .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000))
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        );

        signedJWT.sign(new MACSigner("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"));
        String tokenWithoutScope = signedJWT.serialize();

        when(userInfoService.getUserInfo(eq(tokenWithoutScope), eq(List.of())))
            .thenReturn(userInfoResponse);

        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer " + tokenWithoutScope))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("user123"));
    }
}