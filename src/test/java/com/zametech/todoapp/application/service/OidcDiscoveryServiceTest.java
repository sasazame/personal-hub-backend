package com.zametech.todoapp.application.service;

import com.zametech.todoapp.presentation.dto.oidc.OidcDiscoveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OidcDiscoveryServiceTest {

    @InjectMocks
    private OidcDiscoveryService oidcDiscoveryService;

    private final String issuer = "https://auth.example.com";
    private final String baseUrl = "https://auth.example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oidcDiscoveryService, "issuer", issuer);
        ReflectionTestUtils.setField(oidcDiscoveryService, "baseUrl", baseUrl);
    }

    @Test
    void getDiscoveryDocument_ReturnsCompleteOidcConfiguration() {
        // When
        OidcDiscoveryResponse response = oidcDiscoveryService.getDiscoveryDocument();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.issuer()).isEqualTo(issuer);
        assertThat(response.authorizationEndpoint()).isEqualTo(baseUrl + "/auth/authorize");
        assertThat(response.tokenEndpoint()).isEqualTo(baseUrl + "/auth/token");
        assertThat(response.userinfoEndpoint()).isEqualTo(baseUrl + "/auth/userinfo");
        assertThat(response.jwksUri()).isEqualTo(baseUrl + "/.well-known/jwks.json");
        assertThat(response.registrationEndpoint()).isEqualTo(baseUrl + "/auth/register");
        
        // Check supported scopes
        assertThat(response.scopesSupported()).containsExactly("openid", "profile", "email", "offline_access");
        
        // Check response types
        assertThat(response.responseTypesSupported()).containsExactly("code", "id_token", "token id_token");
        
        // Check response modes
        assertThat(response.responseModesSupported()).containsExactly("query", "fragment", "form_post");
        
        // Check grant types
        assertThat(response.grantTypesSupported()).containsExactly("authorization_code", "implicit", "refresh_token");
        
        // Check subject types
        assertThat(response.subjectTypesSupported()).containsExactly("public");
        
        // Check signing algorithms
        assertThat(response.idTokenSigningAlgValuesSupported()).containsExactly("RS256", "ES256");
        assertThat(response.userinfoSigningAlgValuesSupported()).containsExactly("RS256", "ES256");
        assertThat(response.requestObjectSigningAlgValuesSupported()).containsExactly("RS256", "ES256");
        assertThat(response.tokenEndpointAuthSigningAlgValuesSupported()).containsExactly("RS256", "ES256");
        
        // Check empty lists
        assertThat(response.acrValuesSupported()).isEmpty();
        assertThat(response.idTokenEncryptionAlgValuesSupported()).isEmpty();
        assertThat(response.idTokenEncryptionEncValuesSupported()).isEmpty();
        assertThat(response.userinfoEncryptionAlgValuesSupported()).isEmpty();
        assertThat(response.userinfoEncryptionEncValuesSupported()).isEmpty();
        assertThat(response.requestObjectEncryptionAlgValuesSupported()).isEmpty();
        assertThat(response.requestObjectEncryptionEncValuesSupported()).isEmpty();
        
        // Check token endpoint auth methods
        assertThat(response.tokenEndpointAuthMethodsSupported())
            .containsExactly("client_secret_basic", "client_secret_post", "none");
        
        // Check display values
        assertThat(response.displayValuesSupported()).containsExactly("page", "popup", "touch", "wap");
        
        // Check claim types
        assertThat(response.claimTypesSupported()).containsExactly("normal");
        
        // Check claims supported
        assertThat(response.claimsSupported()).contains(
            "sub", "name", "given_name", "family_name", "email", "email_verified"
        );
        
        // Check documentation and policy URLs
        assertThat(response.serviceDocumentation()).isEqualTo(baseUrl + "/docs");
        assertThat(response.opPolicyUri()).isEqualTo(baseUrl + "/policy");
        assertThat(response.opTosUri()).isEqualTo(baseUrl + "/terms");
        
        // Check locales
        assertThat(response.claimsLocalesSupported()).containsExactly("en-US", "ja-JP");
        assertThat(response.uiLocalesSupported()).containsExactly("en", "ja");
        
        // Check boolean parameters
        assertThat(response.claimsParameterSupported()).isTrue();
        assertThat(response.requestParameterSupported()).isTrue();
        assertThat(response.requestUriParameterSupported()).isFalse();
        assertThat(response.requireRequestUriRegistration()).isFalse();
        
        // Check code challenge methods
        assertThat(response.codeChallengeMethodsSupported()).containsExactly("plain", "S256");
    }
}