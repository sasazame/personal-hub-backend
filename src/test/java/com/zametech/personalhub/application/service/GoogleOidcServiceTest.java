package com.zametech.personalhub.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.common.exception.DuplicateAuthorizationCodeException;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.model.UserSocialAccount;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.domain.repository.UserSocialAccountRepository;
import com.zametech.personalhub.infrastructure.security.TokenEncryptionService;
import com.zametech.personalhub.presentation.dto.request.OidcCallbackRequest;
import com.zametech.personalhub.presentation.dto.response.AuthenticationResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOidcServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserSocialAccountRepository socialAccountRepository;
    
    @Mock
    private OidcTokenService tokenService;
    
    @Mock
    private SecurityEventService securityEventService;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private TokenEncryptionService tokenEncryptionService;
    
    @Mock
    private OAuthCodeCacheService codeCache;
    
    @InjectMocks
    private GoogleOidcService googleOidcService;
    
    private String clientId = "test-client-id";
    private String clientSecret = "test-client-secret";
    private String redirectUri = "http://localhost:3000/callback";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleOidcService, "clientId", clientId);
        ReflectionTestUtils.setField(googleOidcService, "clientSecret", clientSecret);
        ReflectionTestUtils.setField(googleOidcService, "redirectUri", redirectUri);
    }
    
    @Test
    void generateAuthorizationUrl_withValidParameters_shouldReturnCorrectUrl() {
        // Given
        String state = "test-state";
        String nonce = "test-nonce";
        
        // When
        String url = googleOidcService.generateAuthorizationUrl(state, nonce);
        
        // Then
        assertThat(url).isNotNull();
        assertThat(url).contains("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(url).contains("client_id=" + clientId);
        assertThat(url).contains("redirect_uri=" + redirectUri);
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=openid email profile https://www.googleapis.com/auth/calendar");
        assertThat(url).contains("state=" + state);
        assertThat(url).contains("nonce=" + nonce);
        assertThat(url).contains("access_type=offline");
        assertThat(url).contains("prompt=consent");
    }
    
    @Test
    void generateAuthorizationUrl_withNoClientId_shouldThrowException() {
        // Given
        ReflectionTestUtils.setField(googleOidcService, "clientId", "");
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.generateAuthorizationUrl("state", "nonce"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Google OAuth client ID is not configured");
    }
    
    @Test
    void handleCallback_withNewUser_shouldCreateUserAndReturnToken() {
        // Given
        String authCode = "test-auth-code";
        String ipAddress = "127.0.0.1";
        String userAgent = "Test Browser";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock token exchange
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(null);
        
        ResponseEntity<Map> tokenResponseEntity = ResponseEntity.ok(tokenResponse);
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);
        
        // Mock user info fetch
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "test@example.com",
            true,
            "Test User",
            "Test",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        ResponseEntity<GoogleUserInfo> userInfoResponse = ResponseEntity.ok(userInfo);
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(GoogleUserInfo.class)
        )).thenReturn(userInfoResponse);
        
        // Mock user repository
        when(socialAccountRepository.findByProviderAndProviderUserId("google", "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("test")).thenReturn(false);
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("test@example.com");
        savedUser.setUsername("test");
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Mock token service
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        
        // Mock token encryption
        when(tokenEncryptionService.encryptToken(anyString())).thenReturn("encrypted-token");
        
        // Mock object mapper
        when(objectMapper.convertValue(any(GoogleUserInfo.class), eq(Map.class)))
            .thenReturn(new HashMap<>());
        
        // When
        AuthenticationResponse response = googleOidcService.handleCallback(request, ipAddress, userAgent);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().email()).isEqualTo("test@example.com");
        
        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getEmailVerified()).isTrue();
        assertThat(createdUser.getGivenName()).isEqualTo("Test");
        assertThat(createdUser.getFamilyName()).isEqualTo("User");
        
        // Verify social account creation
        verify(socialAccountRepository).save(any(UserSocialAccount.class));
        
        // Verify security event
        verify(securityEventService).recordLoginSuccess(any(User.class), eq("google"), 
            eq(ipAddress), eq(userAgent), isNull());
    }
    
    @Test
    void handleCallback_withExistingUser_shouldLinkAccountAndReturnToken() {
        // Given
        String authCode = "test-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock token exchange
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(null);
        
        ResponseEntity<Map> tokenResponseEntity = ResponseEntity.ok(tokenResponse);
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);
        
        // Mock user info
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "existing@example.com",
            true,
            "Existing User",
            "Existing",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        ResponseEntity<GoogleUserInfo> userInfoResponse = ResponseEntity.ok(userInfo);
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(GoogleUserInfo.class)
        )).thenReturn(userInfoResponse);
        
        // Mock existing user
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("existing@example.com");
        existingUser.setUsername("existing");
        existingUser.setEmailVerified(false); // Not verified before
        
        when(socialAccountRepository.findByProviderAndProviderUserId("google", "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        
        // Mock other services
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(tokenEncryptionService.encryptToken(anyString())).thenReturn("encrypted-token");
        when(objectMapper.convertValue(any(GoogleUserInfo.class), eq(Map.class)))
            .thenReturn(new HashMap<>());
        
        // When
        AuthenticationResponse response = googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        
        // Verify user was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getEmailVerified()).isTrue(); // Should be updated
        assertThat(updatedUser.getGivenName()).isEqualTo("Existing");
        assertThat(updatedUser.getFamilyName()).isEqualTo("User");
    }
    
    @Test
    void handleCallback_withExistingSocialAccount_shouldReturnExistingUser() {
        // Given
        String authCode = "test-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock token exchange
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("refresh_token", "test-refresh-token");
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(null);
        
        ResponseEntity<Map> tokenResponseEntity = ResponseEntity.ok(tokenResponse);
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);
        
        // Mock user info
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "existing@example.com",
            true,
            "Existing User",
            "Existing",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        ResponseEntity<GoogleUserInfo> userInfoResponse = ResponseEntity.ok(userInfo);
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(GoogleUserInfo.class)
        )).thenReturn(userInfoResponse);
        
        // Mock existing social account
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("existing@example.com");
        
        UserSocialAccount existingSocialAccount = new UserSocialAccount();
        existingSocialAccount.setUser(existingUser);
        
        when(socialAccountRepository.findByProviderAndProviderUserId("google", "google-123"))
            .thenReturn(Optional.of(existingSocialAccount));
        
        // Mock other services
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(tokenEncryptionService.encryptToken(anyString())).thenReturn("encrypted-token");
        when(objectMapper.convertValue(any(GoogleUserInfo.class), eq(Map.class)))
            .thenReturn(new HashMap<>());
        
        // When
        AuthenticationResponse response = googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        
        // Verify no new user was created
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void handleCallback_withDuplicateAuthCode_shouldUseCachedResponse() {
        // Given
        String authCode = "duplicate-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock cached token response
        Map<String, Object> cachedTokenResponse = new HashMap<>();
        cachedTokenResponse.put("access_token", "cached-access-token");
        cachedTokenResponse.put("id_token", "cached-id-token");
        cachedTokenResponse.put("refresh_token", "cached-refresh-token");
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(cachedTokenResponse);
        
        // Mock user info
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "test@example.com",
            true,
            "Test User",
            "Test",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        ResponseEntity<GoogleUserInfo> userInfoResponse = ResponseEntity.ok(userInfo);
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(GoogleUserInfo.class)
        )).thenReturn(userInfoResponse);
        
        // Mock user creation
        when(socialAccountRepository.findByProviderAndProviderUserId("google", "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("test")).thenReturn(false);
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("test@example.com");
        savedUser.setUsername("test");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(tokenEncryptionService.encryptToken(anyString())).thenReturn("encrypted-token");
        when(objectMapper.convertValue(any(GoogleUserInfo.class), eq(Map.class)))
            .thenReturn(new HashMap<>());
        
        // When
        AuthenticationResponse response = googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        
        // Verify no token exchange was made (used cache instead)
        verify(restTemplate, never()).exchange(
            eq("https://oauth2.googleapis.com/token"),
            any(), any(), eq(Map.class)
        );
    }
    
    @Test
    void handleCallback_withFailedAuthCode_shouldThrowDuplicateException() {
        // Given
        String authCode = "failed-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock failed cached response (empty map indicates failure)
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(new HashMap<>());
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Google authentication failed")
            .hasCauseInstanceOf(DuplicateAuthorizationCodeException.class);
    }
    
    @Test
    void handleCallback_whenTokenExchangeFails_shouldThrowException() {
        // Given
        String authCode = "test-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(null);
        
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RestClientException("Network error"));
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Google authentication failed");
        
        // Verify code was marked as failed
        verify(codeCache).markCodeAsFailed(authCode);
        
        // Verify security event was recorded
        verify(securityEventService).recordLoginFailure(isNull(), eq("google"), 
            eq("127.0.0.1"), eq("Test Browser"), eq("GOOGLE_AUTH_FAILED"), 
            anyString(), isNull());
    }
    
    @Test
    void handleCallback_whenUserInfoFetchFails_shouldThrowException() {
        // Given
        String authCode = "test-auth-code";
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock successful token exchange
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-access-token");
        
        when(codeCache.getCachedTokenResponse(authCode)).thenReturn(null);
        
        ResponseEntity<Map> tokenResponseEntity = ResponseEntity.ok(tokenResponse);
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(tokenResponseEntity);
        
        // Mock failed user info fetch
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(GoogleUserInfo.class)
        )).thenThrow(new RestClientException("API error"));
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.handleCallback(request, "127.0.0.1", "Test Browser"))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Google authentication failed");
    }
    
    @Test
    void generateUsername_withExistingUsername_shouldAppendNumber() {
        // Given
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "test@example.com",
            true,
            "Test User",
            "Test",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        // Set up private method access
        when(userRepository.existsByUsername("test")).thenReturn(true);
        when(userRepository.existsByUsername("test1")).thenReturn(true);
        when(userRepository.existsByUsername("test2")).thenReturn(false);
        
        // Use reflection to test private method
        String username = ReflectionTestUtils.invokeMethod(googleOidcService, "generateUsername", userInfo);
        
        // Then
        assertThat(username).isEqualTo("test2");
    }
    
    @Test
    void refreshAccessToken_withValidRefreshToken_shouldUpdateToken() {
        // Given
        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.setRefreshTokenEncrypted("encrypted-refresh-token");
        User user = new User();
        user.setId(UUID.randomUUID());
        socialAccount.setUser(user);
        
        when(tokenEncryptionService.decryptToken("encrypted-refresh-token"))
            .thenReturn("decrypted-refresh-token");
        
        // Mock token refresh response
        Map<String, Object> refreshResponse = new HashMap<>();
        refreshResponse.put("access_token", "new-access-token");
        refreshResponse.put("expires_in", 3600);
        
        ResponseEntity<Map> refreshResponseEntity = ResponseEntity.ok(refreshResponse);
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(refreshResponseEntity);
        
        when(tokenEncryptionService.encryptToken("new-access-token"))
            .thenReturn("encrypted-new-access-token");
        
        // When
        googleOidcService.refreshAccessToken(socialAccount);
        
        // Then
        assertThat(socialAccount.getAccessTokenEncrypted()).isEqualTo("encrypted-new-access-token");
        assertThat(socialAccount.getTokenExpiresAt()).isNotNull();
        assertThat(socialAccount.getTokenExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(socialAccount.getTokenExpiresAt()).isBefore(LocalDateTime.now().plusHours(2));
        
        verify(socialAccountRepository).save(socialAccount);
    }
    
    @Test
    void refreshAccessToken_withNoRefreshToken_shouldThrowException() {
        // Given
        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.setRefreshTokenEncrypted(null);
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.refreshAccessToken(socialAccount))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No refresh token available for user");
    }
    
    @Test
    void refreshAccessToken_whenRefreshFails_shouldThrowException() {
        // Given
        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.setRefreshTokenEncrypted("encrypted-refresh-token");
        User user = new User();
        user.setId(UUID.randomUUID());
        socialAccount.setUser(user);
        
        when(tokenEncryptionService.decryptToken("encrypted-refresh-token"))
            .thenReturn("decrypted-refresh-token");
        
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RestClientException("Token refresh failed"));
        
        // When & Then
        assertThatThrownBy(() -> googleOidcService.refreshAccessToken(socialAccount))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to refresh access token");
    }
    
    @Test
    void saveSocialAccount_withExistingAccount_shouldUpdate() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        
        GoogleUserInfo userInfo = new GoogleUserInfo(
            "google-123",
            "test@example.com",
            true,
            "Test User",
            "Test",
            "User",
            "https://example.com/avatar.jpg",
            "en"
        );
        
        UserSocialAccount existingAccount = new UserSocialAccount();
        existingAccount.setUser(user);
        existingAccount.setProvider("google");
        
        when(socialAccountRepository.findByUserIdAndProvider(user.getId(), "google"))
            .thenReturn(Optional.of(existingAccount));
        
        when(objectMapper.convertValue(userInfo, Map.class)).thenReturn(new HashMap<>());
        when(tokenEncryptionService.encryptToken(anyString())).thenReturn("encrypted-token");
        
        // Use reflection to test private method
        ReflectionTestUtils.invokeMethod(googleOidcService, "saveSocialAccount", 
            user, userInfo, "access-token", "refresh-token");
        
        // Then
        verify(socialAccountRepository).save(existingAccount);
        assertThat(existingAccount.getUpdatedAt()).isNotNull();
        assertThat(existingAccount.getAccessTokenEncrypted()).isEqualTo("encrypted-token");
        assertThat(existingAccount.getRefreshTokenEncrypted()).isEqualTo("encrypted-token");
    }
}