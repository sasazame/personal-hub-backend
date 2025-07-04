package com.zametech.personalhub.integration;

import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.infrastructure.security.JwtService;
import com.zametech.personalhub.presentation.dto.request.ChangePasswordRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String authToken;
    private User testUser;
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String NEW_PASSWORD = "NewPassword123!";

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Generate auth token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities(new ArrayList<>())
                .build();
        authToken = jwtService.generateToken(userDetails);
    }

    @Test
    void getUserProfile_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUserProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserProfile_Success() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com",
                TEST_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("newemail@example.com"));

        // Verify changes in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo("newusername");
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPassword())).isTrue();
    }

    @Test
    void updateUserProfile_InvalidPassword() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                null,
                "wrongpassword",
                null
        );

        mockMvc.perform(put("/api/v1/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                TEST_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(put("/api/v1/users/{id}/password", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Verify password changed in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPassword())).isTrue();
    }

    @Test
    void changePassword_WeakPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                TEST_PASSWORD,
                "weak"  // Too weak password
        );

        mockMvc.perform(put("/api/v1/users/{id}/password", testUser.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify user deleted from database
        assertThat(userRepository.findById(testUser.getId())).isEmpty();
    }

    @Test
    void accessOtherUserProfile_Forbidden() throws Exception {
        // Create another user
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setUsername("otheruser");
        otherUser.setPassword(passwordEncoder.encode("OtherPass123!"));
        otherUser.setEnabled(true);
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);

        // Try to access other user's profile
        mockMvc.perform(get("/api/v1/users/{id}", otherUser.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
}
