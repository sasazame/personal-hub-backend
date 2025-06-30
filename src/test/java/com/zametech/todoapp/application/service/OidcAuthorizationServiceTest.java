package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.AuthorizationCode;
import com.zametech.todoapp.domain.model.OAuthApplication;
import com.zametech.todoapp.domain.model.SecurityEvent;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.AuthorizationCodeRepository;
import com.zametech.todoapp.domain.repository.OAuthApplicationRepository;
import com.zametech.todoapp.presentation.dto.oidc.AuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcAuthorizationServiceTest {
    
    @Mock
    private OAuthApplicationRepository oAuthApplicationRepository;
    
    @Mock
    private AuthorizationCodeRepository authorizationCodeRepository;
    
    @Mock
    private PkceService pkceService;
    
    @Mock
    private SecurityEventService securityEventService;
    
    @InjectMocks
    private OidcAuthorizationService oidcAuthorizationService;
    
    private User testUser;
    private OAuthApplication testApplication;
    private AuthorizationRequest validRequest;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oidcAuthorizationService, "authorizationCodeTtl", 600);
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        
        testApplication = new OAuthApplication();
        testApplication.setId(UUID.randomUUID());
        testApplication.setClientId("test-client");
        testApplication.setClientSecretHash("test-secret-hash");
        testApplication.setRedirectUris(Arrays.asList("http://localhost:3000/callback", "http://localhost:3001/callback"));
        testApplication.setScopes(Arrays.asList("openid", "email", "profile"));
        
        validRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .scope("openid email")
            .state("test-state")
            .nonce("test-nonce")
            .build();
    }
    
    @Test
    void generateAuthorizationCode_withValidRequest_shouldReturnCode() {
        // Given
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        when(authorizationCodeRepository.save(any(AuthorizationCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        String code = oidcAuthorizationService.generateAuthorizationCode(validRequest, testUser);
        
        // Then
        assertThat(code).isNotNull();
        assertThat(code).isNotEmpty();
        
        // Verify authorization code was saved
        ArgumentCaptor<AuthorizationCode> authCodeCaptor = ArgumentCaptor.forClass(AuthorizationCode.class);
        verify(authorizationCodeRepository).save(authCodeCaptor.capture());
        
        AuthorizationCode savedCode = authCodeCaptor.getValue();
        assertThat(savedCode.getCode()).isEqualTo(code);
        assertThat(savedCode.getClientId()).isEqualTo("test-client");
        assertThat(savedCode.getUser()).isEqualTo(testUser);
        assertThat(savedCode.getRedirectUri()).isEqualTo("http://localhost:3000/callback");
        assertThat(savedCode.getScopes()).containsExactly("openid", "email");
        assertThat(savedCode.getState()).isEqualTo("test-state");
        assertThat(savedCode.getNonce()).isEqualTo("test-nonce");
        assertThat(savedCode.getExpiresAt()).isAfter(LocalDateTime.now());
        
        // Verify security event was logged
        verify(securityEventService).logSecurityEvent(
            SecurityEvent.EventType.AUTHORIZATION_CODE_ISSUED,
            testUser,
            "test-client",
            true
        );
    }
    
    @Test
    void generateAuthorizationCode_withPkce_shouldIncludeChallengeInfo() {
        // Given
        AuthorizationRequest pkceRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .scope("openid")
            .codeChallenge("test-challenge")
            .codeChallengeMethod("S256")
            .build();
        
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        when(authorizationCodeRepository.save(any(AuthorizationCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        String code = oidcAuthorizationService.generateAuthorizationCode(pkceRequest, testUser);
        
        // Then
        assertThat(code).isNotNull();
        
        ArgumentCaptor<AuthorizationCode> authCodeCaptor = ArgumentCaptor.forClass(AuthorizationCode.class);
        verify(authorizationCodeRepository).save(authCodeCaptor.capture());
        
        AuthorizationCode savedCode = authCodeCaptor.getValue();
        assertThat(savedCode.getCodeChallenge()).isEqualTo("test-challenge");
        assertThat(savedCode.getCodeChallengeMethod()).isEqualTo("S256");
    }
    
    @Test
    void generateAuthorizationCode_withInvalidClientId_shouldThrowException() {
        // Given
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(validRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid client_id");
    }
    
    @Test
    void generateAuthorizationCode_withInvalidRedirectUri_shouldThrowException() {
        // Given
        AuthorizationRequest requestWithBadUri = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://evil.com/callback")
            .responseType("code")
            .scope("openid")
            .build();
        
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(requestWithBadUri, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid redirect_uri");
    }
    
    @Test
    void generateAuthorizationCode_withInvalidScope_shouldThrowException() {
        // Given
        AuthorizationRequest requestWithBadScope = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .scope("openid invalid_scope")
            .build();
        
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(requestWithBadScope, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid scope: invalid_scope");
    }
    
    @Test
    void generateAuthorizationCode_withMissingClientId_shouldThrowException() {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(invalidRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("client_id is required");
    }
    
    @Test
    void generateAuthorizationCode_withMissingRedirectUri_shouldThrowException() {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .responseType("code")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(invalidRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("redirect_uri is required");
    }
    
    @Test
    void generateAuthorizationCode_withInvalidResponseType_shouldThrowException() {
        // Given
        AuthorizationRequest invalidRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("token")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(invalidRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only 'code' response type is supported");
    }
    
    @Test
    void generateAuthorizationCode_withInvalidPkce_shouldThrowException() {
        // Given
        AuthorizationRequest invalidPkceRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .codeChallenge("")
            .codeChallengeMethod("S256")
            .build();
        
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(invalidPkceRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("code_challenge is required when using PKCE");
    }
    
    @Test
    void generateAuthorizationCode_withUnsupportedCodeChallengeMethod_shouldThrowException() {
        // Given
        AuthorizationRequest invalidPkceRequest = AuthorizationRequest.builder()
            .clientId("test-client")
            .redirectUri("http://localhost:3000/callback")
            .responseType("code")
            .codeChallenge("test-challenge")
            .codeChallengeMethod("unsupported")
            .build();
        
        when(oAuthApplicationRepository.findByClientId("test-client"))
            .thenReturn(Optional.of(testApplication));
        
        // When & Then
        assertThatThrownBy(() -> oidcAuthorizationService.generateAuthorizationCode(invalidPkceRequest, testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported code_challenge_method: unsupported");
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withValidCode_shouldReturnAuthCode() {
        // Given
        String code = "test-auth-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:3000/callback";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri(redirectUri)
            .scopes(Arrays.asList("openid", "email"))
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        when(authorizationCodeRepository.save(any(AuthorizationCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, redirectUri, null);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(authCode);
        assertThat(authCode.getUsed()).isTrue();
        
        verify(authorizationCodeRepository).save(authCode);
        verify(securityEventService).logSecurityEvent(
            SecurityEvent.EventType.AUTHORIZATION_CODE_USED,
            testUser,
            clientId,
            true
        );
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withPkce_shouldVerifyCodeChallenge() {
        // Given
        String code = "test-auth-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:3000/callback";
        String codeVerifier = "test-verifier";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri(redirectUri)
            .scopes(Arrays.asList("openid"))
            .codeChallenge("test-challenge")
            .codeChallengeMethod("S256")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        when(pkceService.verifyCodeChallenge(codeVerifier, "test-challenge", "S256"))
            .thenReturn(true);
        when(authorizationCodeRepository.save(any(AuthorizationCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, redirectUri, codeVerifier);
        
        // Then
        assertThat(result).isPresent();
        verify(pkceService).verifyCodeChallenge(codeVerifier, "test-challenge", "S256");
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withInvalidCode_shouldReturnEmpty() {
        // Given
        String code = "invalid-code";
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.empty());
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, "test-client", "http://localhost:3000/callback", null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withExpiredCode_shouldReturnEmpty() {
        // Given
        String code = "expired-code";
        String clientId = "test-client";
        
        AuthorizationCode expiredCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri("http://localhost:3000/callback")
            .authTime(LocalDateTime.now().minusMinutes(20))
            .expiresAt(LocalDateTime.now().minusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(expiredCode));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, "http://localhost:3000/callback", null);
        
        // Then
        assertThat(result).isEmpty();
        
        verify(securityEventService).logSecurityEvent(
            eq(SecurityEvent.EventType.AUTHORIZATION_CODE_EXPIRED),
            eq(testUser),
            eq(clientId),
            eq(false),
            eq("invalid_grant"),
            eq("Authorization code is invalid or expired"),
            isNull()
        );
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withAlreadyUsedCode_shouldReturnEmpty() {
        // Given
        String code = "used-code";
        String clientId = "test-client";
        
        AuthorizationCode usedCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri("http://localhost:3000/callback")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .used(true)
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(usedCode));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, "http://localhost:3000/callback", null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withClientIdMismatch_shouldReturnEmpty() {
        // Given
        String code = "test-code";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId("different-client")
            .user(testUser)
            .redirectUri("http://localhost:3000/callback")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, "test-client", "http://localhost:3000/callback", null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withRedirectUriMismatch_shouldReturnEmpty() {
        // Given
        String code = "test-code";
        String clientId = "test-client";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri("http://localhost:3000/callback")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, "http://localhost:3001/callback", null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withMissingCodeVerifier_shouldReturnEmpty() {
        // Given
        String code = "test-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:3000/callback";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri(redirectUri)
            .codeChallenge("test-challenge")
            .codeChallengeMethod("S256")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, redirectUri, null);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void validateAndConsumeAuthorizationCode_withFailedPkceVerification_shouldReturnEmpty() {
        // Given
        String code = "test-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:3000/callback";
        String codeVerifier = "wrong-verifier";
        
        AuthorizationCode authCode = AuthorizationCode.builder()
            .code(code)
            .clientId(clientId)
            .user(testUser)
            .redirectUri(redirectUri)
            .codeChallenge("test-challenge")
            .codeChallengeMethod("S256")
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
        
        when(authorizationCodeRepository.findByCode(code))
            .thenReturn(Optional.of(authCode));
        when(pkceService.verifyCodeChallenge(codeVerifier, "test-challenge", "S256"))
            .thenReturn(false);
        
        // When
        Optional<AuthorizationCode> result = oidcAuthorizationService.validateAndConsumeAuthorizationCode(
            code, clientId, redirectUri, codeVerifier);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void parseScopes_withNullScope_shouldReturnDefaultOpenid() {
        // Given & When
        List<String> scopes = ReflectionTestUtils.invokeMethod(
            oidcAuthorizationService, "parseScopes", (String) null);
        
        // Then
        assertThat(scopes).containsExactly("openid");
    }
    
    @Test
    void parseScopes_withEmptyScope_shouldReturnDefaultOpenid() {
        // Given & When
        List<String> scopes = ReflectionTestUtils.invokeMethod(
            oidcAuthorizationService, "parseScopes", "");
        
        // Then
        assertThat(scopes).containsExactly("openid");
    }
    
    @Test
    void parseScopes_withMultipleScopes_shouldReturnParsedList() {
        // Given & When
        List<String> scopes = ReflectionTestUtils.invokeMethod(
            oidcAuthorizationService, "parseScopes", "openid email profile");
        
        // Then
        assertThat(scopes).containsExactly("openid", "email", "profile");
    }
    
    @Test
    void generateSecureCode_shouldReturnBase64UrlEncodedString() {
        // Given & When
        String code = ReflectionTestUtils.invokeMethod(oidcAuthorizationService, "generateSecureCode");
        
        // Then
        assertThat(code).isNotNull();
        assertThat(code).isNotEmpty();
        assertThat(code).matches("^[A-Za-z0-9_-]+$"); // Base64 URL-safe characters
        assertThat(code.length()).isGreaterThanOrEqualTo(32); // At least 32 bytes encoded
    }
}