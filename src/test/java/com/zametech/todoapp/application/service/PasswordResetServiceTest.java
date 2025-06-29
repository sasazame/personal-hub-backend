package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.PasswordResetToken;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.PasswordResetTokenRepository;
import com.zametech.todoapp.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setUsername("testuser");

        testToken = new PasswordResetToken();
        testToken.setId(UUID.randomUUID());
        testToken.setToken("test-token");
        testToken.setUser(testUser);
        testToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        testToken.setUsed(false);
        testToken.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void requestPasswordReset_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(testUser.getEmail()));

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(passwordResetTokenRepository).deleteByUserId(testUser.getId());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(testUser.getEmail()), anyString());
    }

    @Test
    void requestPasswordReset_UserNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.requestPasswordReset("nonexistent@example.com"));

        assertTrue(exception.getMessage().contains("If an account with this email exists"));
        verify(userRepository).findByEmail("nonexistent@example.com");
        verifyNoInteractions(passwordResetTokenRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPassword_Success() {
        String newPassword = "newPassword123";
        String encodedPassword = "encodedNewPassword";

        when(passwordResetTokenRepository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(testToken);

        assertDoesNotThrow(() -> passwordResetService.resetPassword(testToken.getToken(), newPassword));

        verify(passwordResetTokenRepository).findByToken(testToken.getToken());
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(argThat(user -> 
            user.getPassword().equals(encodedPassword) && 
            user.getUpdatedAt() != null
        ));
        verify(passwordResetTokenRepository).save(argThat(token -> token.isUsed()));
    }

    @Test
    void resetPassword_InvalidToken() {
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.resetPassword("invalid-token", "newPassword"));

        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken("invalid-token");
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPassword_ExpiredToken() {
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.resetPassword(testToken.getToken(), "newPassword"));

        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken.getToken());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPassword_UsedToken() {
        testToken.setUsed(true);
        when(passwordResetTokenRepository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.resetPassword(testToken.getToken(), "newPassword"));

        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken.getToken());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateToken_Success() {
        when(passwordResetTokenRepository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));

        assertDoesNotThrow(() -> passwordResetService.validateToken(testToken.getToken()));

        verify(passwordResetTokenRepository).findByToken(testToken.getToken());
    }

    @Test
    void validateToken_InvalidToken() {
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.validateToken("invalid-token"));

        assertEquals("Invalid reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken("invalid-token");
    }

    @Test
    void validateToken_ExpiredToken() {
        testToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken(testToken.getToken())).thenReturn(Optional.of(testToken));

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> passwordResetService.validateToken(testToken.getToken()));

        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(passwordResetTokenRepository).findByToken(testToken.getToken());
    }

    @Test
    void cleanupExpiredTokens_Success() {
        assertDoesNotThrow(() -> passwordResetService.cleanupExpiredTokens());

        verify(passwordResetTokenRepository).deleteExpiredTokens();
    }
}