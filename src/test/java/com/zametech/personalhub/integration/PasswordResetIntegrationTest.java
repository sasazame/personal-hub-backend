package com.zametech.personalhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.domain.model.PasswordResetToken;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.PasswordResetTokenRepository;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.presentation.dto.request.ForgotPasswordRequest;
import com.zametech.personalhub.presentation.dto.request.ResetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("oldPassword123"));
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account with this email exists, you will receive a password reset email."));

        // Verify token was created
        var tokens = passwordResetTokenRepository.findByUserId(testUser.getId());
        assertTrue(tokens.isPresent());
        assertFalse(tokens.get().isUsed());
        assertTrue(tokens.get().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void forgotPassword_NonExistentEmail_ReturnsSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account with this email exists, you will receive a password reset email."));
    }

    @Test
    void forgotPassword_InvalidEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_Success() throws Exception {
        // Create password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("valid-reset-token");
        resetToken.setUser(testUser);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        resetToken = passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest("valid-reset-token", "NewPassword123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been successfully reset. You can now login with your new password."));

        // Verify password was changed
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPassword123", updatedUser.getPassword()));

        // Verify token was marked as used
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertTrue(updatedToken.isUsed());
    }

    @Test
    void resetPassword_InvalidToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "NewPassword123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void resetPassword_ExpiredToken() throws Exception {
        // Create expired token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("expired-token");
        resetToken.setUser(testUser);
        resetToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPassword123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void resetPassword_WeakPassword() throws Exception {
        // Create password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("valid-token-weak");
        resetToken.setUser(testUser);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        ResetPasswordRequest request = new ResetPasswordRequest("valid-token-weak", "weak");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateResetToken_Success() throws Exception {
        // Create password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("valid-token-validate");
        resetToken.setUser(testUser);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        mockMvc.perform(get("/api/v1/auth/validate-reset-token")
                .param("token", "valid-token-validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateResetToken_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate-reset-token")
                .param("token", "invalid-token"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void validateResetToken_ExpiredToken() throws Exception {
        // Create expired token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("expired-validate-token");
        resetToken.setUser(testUser);
        resetToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        mockMvc.perform(get("/api/v1/auth/validate-reset-token")
                .param("token", "expired-validate-token"))
                .andExpect(status().isInternalServerError());
    }
}
