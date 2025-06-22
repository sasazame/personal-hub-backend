package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.GoogleOidcService;
import com.zametech.todoapp.presentation.dto.request.OidcCallbackRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcControllerTest {
    
    @Mock
    private GoogleOidcService googleOidcService;
    
    @InjectMocks
    private OidcController oidcController;
    
    @Test
    void testInitiateGoogleAuth() {
        ResponseEntity<?> response = oidcController.initiateGoogleAuth();
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    void testHandleGoogleCallback() {
        // Arrange
        OidcCallbackRequest request = new OidcCallbackRequest("test-code", "test-state", null, null);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("127.0.0.1");
        servletRequest.addHeader("User-Agent", "Test Browser");
        
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        AuthenticationResponse authResponse = new AuthenticationResponse(
                "test-jwt-token",
                null,
                userResponse
        );
        
        when(googleOidcService.handleCallback(any(OidcCallbackRequest.class), anyString(), anyString()))
                .thenReturn(authResponse);
        
        // Act
        ResponseEntity<AuthenticationResponse> response = oidcController.handleGoogleCallback(request, servletRequest);
        
        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("test-jwt-token");
        assertThat(response.getBody().getUser().email()).isEqualTo("test@example.com");
    }
}