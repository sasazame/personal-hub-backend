package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.PasswordResetToken;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.PasswordResetTokenRepository;
import com.zametech.todoapp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 1;

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset requested for non-existent email: {}", email);
                    throw new RuntimeException("If an account with this email exists, you will receive a password reset email.");
                });
        
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        String tokenString = generateSecureToken();
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenString);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        resetToken.setUsed(false);
        
        passwordResetTokenRepository.save(resetToken);
        
        emailService.sendPasswordResetEmail(user.getEmail(), tokenString);
        
        log.info("Password reset token generated for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        if (!resetToken.isValid()) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    @Transactional
    public void validateToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));
        
        if (!resetToken.isValid()) {
            throw new RuntimeException("Invalid or expired reset token");
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens();
        log.info("Cleaned up expired password reset tokens");
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}