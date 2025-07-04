package com.zametech.personalhub.application.service;

import com.nimbusds.jwt.SignedJWT;
import com.zametech.personalhub.domain.model.AuthorizationCode;
import com.zametech.personalhub.domain.model.RefreshToken;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.RefreshTokenRepository;
import com.zametech.personalhub.presentation.dto.oidc.TokenRequest;
import com.zametech.personalhub.presentation.dto.oidc.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcTokenServiceTest {

    @Mock
    private OidcAuthorizationService authorizationService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwksService jwksService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OidcTokenService oidcTokenService;

    private User testUser;
    private KeyPair keyPair;
    private AuthorizationCode authCode;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        // Initialize test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setEmailVerified(true);
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setProfilePictureUrl("https://example.com/avatar.jpg");
        testUser.setLocale("en");

        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        // Set up authorization code
        authCode = AuthorizationCode.builder()
            .code("test-auth-code")
            .clientId("test-client")
            .user(testUser)
            .redirectUri("http://localhost:3000/callback")
            .scopes(Arrays.asList("openid", "email", "profile"))
            .nonce("test-nonce")
            .authTime(LocalDateTime.now())
            .build();

        // Set up refresh token
        refreshToken = RefreshToken.builder()
            .tokenHash("hashed-refresh-token")
            .user(testUser)
            .clientId("test-client")
            .scopes(Arrays.asList("openid", "email", "profile"))
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();

        // Set up field values
        ReflectionTestUtils.setField(oidcTokenService, "issuer", "http://localhost:8080");
        ReflectionTestUtils.setField(oidcTokenService, "accessTokenTtl", 900);
        ReflectionTestUtils.setField(oidcTokenService, "refreshTokenTtl", 2592000);
        ReflectionTestUtils.setField(oidcTokenService, "idTokenTtl", 3600);

        // JwksService will be mocked in individual tests when needed
    }

    @Test
    void processTokenRequest_withAuthorizationCodeGrant_shouldReturnTokens() throws Exception {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        TokenRequest request = new TokenRequest(
            "authorization_code",
            "test-auth-code",
            "http://localhost:3000/callback",
            "test-client",
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        when(authorizationService.validateAndConsumeAuthorizationCode(
            "test-auth-code", "test-client", "http://localhost:3000/callback", null
        )).thenReturn(Optional.of(authCode));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TokenResponse response = oidcTokenService.processTokenRequest(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.scope()).isEqualTo("openid email profile");
        assertThat(response.idToken()).isNotNull(); // ID token should be included for openid scope

        // Verify the access token is a valid JWT
        SignedJWT jwt = SignedJWT.parse(response.accessToken());
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("test@example.com");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
    }

    @Test
    void processTokenRequest_withRefreshTokenGrant_shouldReturnNewTokens() {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        TokenRequest request = new TokenRequest(
            "refresh_token",
            null, // code
            null, // redirectUri
            null, // clientId
            null, // clientSecret
            null, // codeVerifier
            "test-refresh-token",
            null  // scope
        );

        // Stub password encoder to handle both lookups and new token creation
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return input.equals("test-refresh-token") ? "hashed-refresh-token" : "new-hashed-token";
        });
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
            .thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TokenResponse response = oidcTokenService.processTokenRequest(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.idToken()).isNull(); // No ID token for refresh grant
        
        // Verify that the old refresh token was revoked
        assertThat(refreshToken.getRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // Once for old token revocation, once for new token
    }

    @Test
    void processTokenRequest_withUnsupportedGrantType_shouldThrowException() {
        // Given
        TokenRequest request = new TokenRequest(
            "implicit",
            null, // code
            null, // redirectUri
            null, // clientId
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        // When & Then
        assertThatThrownBy(() -> oidcTokenService.processTokenRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported grant type: implicit");
    }

    @Test
    void processAuthorizationCodeGrant_withMissingParameters_shouldThrowException() {
        // Given
        TokenRequest request = new TokenRequest(
            "authorization_code",
            null, // Missing code
            "http://localhost:3000/callback",
            "test-client",
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        // When & Then
        assertThatThrownBy(() -> oidcTokenService.processTokenRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing required parameters for authorization_code grant");
    }

    @Test
    void processAuthorizationCodeGrant_withInvalidCode_shouldThrowException() {
        // Given
        TokenRequest request = new TokenRequest(
            "authorization_code",
            "invalid-code",
            "http://localhost:3000/callback",
            "test-client",
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        when(authorizationService.validateAndConsumeAuthorizationCode(
            "invalid-code", "test-client", "http://localhost:3000/callback", null
        )).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> oidcTokenService.processTokenRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid authorization code");
    }

    @Test
    void processRefreshTokenGrant_withMissingToken_shouldThrowException() {
        // Given
        TokenRequest request = new TokenRequest(
            "refresh_token",
            null, // code
            null, // redirectUri
            null, // clientId
            null, // clientSecret
            null, // codeVerifier
            null, // Missing refresh token
            null  // scope
        );

        // When & Then
        assertThatThrownBy(() -> oidcTokenService.processTokenRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing refresh_token parameter");
    }

    @Test
    void processRefreshTokenGrant_withInvalidToken_shouldThrowException() {
        // Given
        TokenRequest request = new TokenRequest(
            "refresh_token",
            null, // code
            null, // redirectUri
            null, // clientId
            null, // clientSecret
            null, // codeVerifier
            "invalid-refresh-token",
            null  // scope
        );

        when(passwordEncoder.encode("invalid-refresh-token")).thenReturn("hashed-invalid-token");
        when(refreshTokenRepository.findByTokenHash("hashed-invalid-token"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> oidcTokenService.processTokenRequest(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid refresh token");
    }

    @Test
    void generateToken_withValidUser_shouldReturnJWT() throws Exception {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        // When
        String token = oidcTokenService.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        
        // Verify JWT structure
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("test@example.com");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(jwt.getJWTClaimsSet().getClaim("email")).isEqualTo("test@example.com");
        assertThat(jwt.getJWTClaimsSet().getClaim("email_verified")).isEqualTo(true);
        assertThat(jwt.getJWTClaimsSet().getClaim("user_id")).isEqualTo(testUser.getId().toString());
    }

    @Test
    void getExpirationTime_shouldReturnCorrectMilliseconds() {
        // When
        long expirationTime = oidcTokenService.getExpirationTime();

        // Then
        assertThat(expirationTime).isEqualTo(900 * 1000L); // 900 seconds in milliseconds
    }

    @Test
    void revokeToken_withValidRefreshToken_shouldReturnTrue() {
        // Given
        String token = "valid-refresh-token";
        String clientId = "test-client";

        // When
        boolean result = oidcTokenService.revokeToken(token, "refresh_token", clientId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void revokeToken_withValidAccessToken_shouldReturnTrue() throws Exception {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        // Generate a token via the normal flow to get client_id claim
        TokenRequest request = new TokenRequest(
            "authorization_code",
            "test-auth-code",
            "http://localhost:3000/callback",
            "test-client",
            null,
            null,
            null,
            null
        );
        
        when(authorizationService.validateAndConsumeAuthorizationCode(
            anyString(), anyString(), anyString(), any()
        )).thenReturn(Optional.of(authCode));
        
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);
        
        TokenResponse tokenResponse = oidcTokenService.processTokenRequest(request);
        String token = tokenResponse.accessToken();

        // When
        boolean result = oidcTokenService.revokeToken(token, "access_token", "test-client");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void revokeToken_withInvalidToken_shouldReturnFalse() {
        // Given
        String token = "invalid-jwt-token";
        String clientId = "test-client";

        // When
        boolean result = oidcTokenService.revokeToken(token, "access_token", clientId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void processAuthorizationCodeGrant_withoutOpenidScope_shouldNotIncludeIdToken() {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        authCode.setScopes(Arrays.asList("email", "profile")); // No openid scope
        
        TokenRequest request = new TokenRequest(
            "authorization_code",
            "test-auth-code",
            "http://localhost:3000/callback",
            "test-client",
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        when(authorizationService.validateAndConsumeAuthorizationCode(
            "test-auth-code", "test-client", "http://localhost:3000/callback", null
        )).thenReturn(Optional.of(authCode));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TokenResponse response = oidcTokenService.processTokenRequest(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.idToken()).isNull(); // No ID token without openid scope
    }

    @Test
    void generateIdToken_withFullUserProfile_shouldIncludeAllClaims() throws Exception {
        // Given
        when(jwksService.getKeyId()).thenReturn("test-key-id");
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        
        TokenRequest request = new TokenRequest(
            "authorization_code",
            "test-auth-code",
            "http://localhost:3000/callback",
            "test-client",
            null, // clientSecret
            null, // codeVerifier
            null, // refreshToken
            null  // scope
        );

        when(authorizationService.validateAndConsumeAuthorizationCode(
            anyString(), anyString(), anyString(), any()
        )).thenReturn(Optional.of(authCode));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TokenResponse response = oidcTokenService.processTokenRequest(request);

        // Then
        assertThat(response.idToken()).isNotNull();
        
        // Verify ID token claims
        SignedJWT idToken = SignedJWT.parse(response.idToken());
        assertThat(idToken.getJWTClaimsSet().getSubject()).isEqualTo("test@example.com");
        assertThat(idToken.getJWTClaimsSet().getClaim("name")).isEqualTo("testuser");
        assertThat(idToken.getJWTClaimsSet().getClaim("given_name")).isEqualTo("Test");
        assertThat(idToken.getJWTClaimsSet().getClaim("family_name")).isEqualTo("User");
        assertThat(idToken.getJWTClaimsSet().getClaim("picture")).isEqualTo("https://example.com/avatar.jpg");
        assertThat(idToken.getJWTClaimsSet().getClaim("locale")).isEqualTo("en");
        assertThat(idToken.getJWTClaimsSet().getClaim("nonce")).isEqualTo("test-nonce");
    }
}