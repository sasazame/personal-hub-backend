package com.zametech.todoapp.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.domain.repository.UserRepository;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.presentation.dto.request.OidcCallbackRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubOAuthServiceTest {
    
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
    
    @InjectMocks
    private GitHubOAuthService gitHubOAuthService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gitHubOAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(gitHubOAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(gitHubOAuthService, "redirectUri", "http://localhost:8080/callback");
    }
    
    @Test
    void generateAuthorizationUrl_shouldReturnCorrectUrl() {
        // Act
        String authUrl = gitHubOAuthService.generateAuthorizationUrl("test-state");
        
        // Assert
        assertThat(authUrl).contains("https://github.com/login/oauth/authorize");
        assertThat(authUrl).contains("client_id=test-client-id");
        assertThat(authUrl).contains("redirect_uri=http://localhost:8080/callback");
        assertThat(authUrl).contains("scope=read:user user:email");
        assertThat(authUrl).contains("state=test-state");
    }
    
    @Test
    void handleCallback_withNewUser_shouldCreateUserAndReturnToken() throws Exception {
        // Arrange
        String authCode = "test-auth-code";
        String accessToken = "test-access-token";
        String jwtToken = "test-jwt-token";
        
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock token exchange
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode tokenResponse = realMapper.createObjectNode()
                .put("access_token", accessToken)
                .put("token_type", "bearer");
        
        when(restTemplate.exchange(
                contains("github.com/login/oauth/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));
        
        // Mock user info
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("login", "testuser");
        userInfo.put("name", "Test User");
        userInfo.put("avatar_url", "https://github.com/avatar.jpg");
        
        when(restTemplate.exchange(
                contains("api.github.com/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(userInfo));
        
        // Mock email info
        ArrayNode emailNode = realMapper.createArrayNode();
        ObjectNode emailObj = realMapper.createObjectNode();
        emailObj.put("email", "test@example.com");
        emailObj.put("primary", true);
        emailObj.put("verified", true);
        emailNode.add(emailObj);
        
        when(restTemplate.exchange(
                contains("api.github.com/user/emails"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(emailNode));
        
        // Mock repository calls
        when(socialAccountRepository.findByProviderAndProviderUserId("github", "12345"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername("testuser"))
                .thenReturn(false);
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("test@example.com");
        savedUser.setUsername("testuser");
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenService.generateToken(any(User.class))).thenReturn(jwtToken);
        
        // Act
        AuthenticationResponse response = gitHubOAuthService.handleCallback(request, "127.0.0.1", "Test Browser");
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(jwtToken);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().email()).isEqualTo("test@example.com");
        assertThat(response.getUser().username()).isEqualTo("testuser");
    }
    
    @Test
    void handleCallback_withExistingUser_shouldReturnToken() throws Exception {
        // Arrange
        String authCode = "test-auth-code";
        String accessToken = "test-access-token";
        String jwtToken = "test-jwt-token";
        
        OidcCallbackRequest request = new OidcCallbackRequest(authCode, "test-state", null, null);
        
        // Mock token exchange
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode tokenResponse = realMapper.createObjectNode()
                .put("access_token", accessToken)
                .put("token_type", "bearer");
        
        when(restTemplate.exchange(
                contains("github.com/login/oauth/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));
        
        // Mock user info
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("login", "testuser");
        userInfo.put("name", "Test User");
        userInfo.put("avatar_url", "https://github.com/avatar.jpg");
        
        when(restTemplate.exchange(
                contains("api.github.com/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(userInfo));
        
        // Mock email info
        ArrayNode emailNode = realMapper.createArrayNode();
        ObjectNode emailObj = realMapper.createObjectNode();
        emailObj.put("email", "test@example.com");
        emailObj.put("primary", true);
        emailObj.put("verified", true);
        emailNode.add(emailObj);
        
        when(restTemplate.exchange(
                contains("api.github.com/user/emails"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(emailNode));
        
        // Mock existing user with social account
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("test@example.com");
        existingUser.setUsername("testuser");
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(existingUser)
                .provider("github")
                .providerUserId("12345")
                .build();
        
        when(socialAccountRepository.findByProviderAndProviderUserId("github", "12345"))
                .thenReturn(Optional.of(socialAccount));
        when(tokenService.generateToken(existingUser)).thenReturn(jwtToken);
        
        // Act
        AuthenticationResponse response = gitHubOAuthService.handleCallback(request, "127.0.0.1", "Test Browser");
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(jwtToken);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().email()).isEqualTo("test@example.com");
    }
    
    @Test
    void handleCallback_withInvalidCode_shouldThrowException() {
        // Arrange
        OidcCallbackRequest request = new OidcCallbackRequest("invalid-code", "test-state", null, null);
        
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode errorResponse = realMapper.createObjectNode()
                .put("error", "bad_verification_code")
                .put("error_description", "The code is incorrect");
        
        when(restTemplate.exchange(
                contains("github.com/login/oauth/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(errorResponse));
        
        // Act & Assert
        assertThatThrownBy(() -> 
                gitHubOAuthService.handleCallback(request, "127.0.0.1", "Test Browser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GitHub authentication failed");
    }
}