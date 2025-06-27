package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.AuthenticationService;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.presentation.dto.request.LoginRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthenticationController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import({TestSecurityConfig.class, com.zametech.todoapp.common.validation.PasswordValidator.class, 
         com.zametech.todoapp.common.validation.StrongPasswordValidator.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;
    
    @MockBean
    private UserContextService userContextService;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setUsername("testuser");
        
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        AuthenticationResponse response = new AuthenticationResponse(
                "jwt-token",
                "refresh-token",
                userResponse
        );

        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRegistrationData() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("123"); // Too weak
        request.setUsername("ab"); // Too short

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        AuthenticationResponse response = new AuthenticationResponse(
                "jwt-token",
                "refresh-token",
                userResponse
        );

        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void shouldReturnBadRequestForInvalidLoginData() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(""); // Empty email
        request.setPassword(""); // Empty password

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldGetCurrentUser() throws Exception {
        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");
        currentUser.setUsername("testuser");
        currentUser.setCreatedAt(LocalDateTime.now());
        currentUser.setUpdatedAt(LocalDateTime.now());
        
        when(userContextService.getCurrentUser()).thenReturn(currentUser);
        
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}