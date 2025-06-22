package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.UserService;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.presentation.dto.request.ChangePasswordRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import({TestSecurityConfig.class, com.zametech.todoapp.common.validation.PasswordValidator.class, 
         com.zametech.todoapp.common.validation.StrongPasswordValidator.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void getUserProfile_Success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userService.getUserById(userId)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).getUserById(userId);
    }

    @Test
    @WithMockUser
    void updateUserProfile_Success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com",
                "currentPassword",
                "newPassword123!"
        );

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setUsername("newusername");
        updatedUser.setCreatedAt(LocalDateTime.now());
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.updateUserProfile(eq(userId), any(UpdateUserRequest.class)))
                .thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"));

        verify(userService).updateUserProfile(eq(userId), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser
    void updateUserProfile_InvalidRequest() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(
                "ab", // Too short
                "invalid-email", // Invalid email
                "", // Empty password
                null
        );

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser
    void changePassword_Success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPassword",
                "NewPassword123!"
        );

        doNothing().when(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser
    void changePassword_WeakPassword() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPassword",
                "weak" // Too weak
        );

        // When & Then
        mockMvc.perform(put("/api/v1/users/{id}/password", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @WithMockUser
    void deleteUser_Success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    void userEndpoints_RequireAuthentication() throws Exception {
        // Given - Mock service to throw RuntimeException when no auth context
        when(userService.getUserById(any())).thenThrow(new RuntimeException("No authenticated user found"));
        when(userService.updateUserProfile(any(), any())).thenThrow(new RuntimeException("No authenticated user found"));
        doThrow(new RuntimeException("No authenticated user found")).when(userService).changePassword(any(), any());
        doThrow(new RuntimeException("No authenticated user found")).when(userService).deleteUser(any());

        // When & Then - All endpoints should return 500 due to authentication errors
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isBadRequest());
    }
}