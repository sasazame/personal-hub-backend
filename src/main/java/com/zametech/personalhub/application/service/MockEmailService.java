package com.zametech.personalhub.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        log.info("=== PASSWORD RESET EMAIL (MOCK) ===");
        log.info("To: {}", toEmail);
        log.info("Subject: Password Reset Request");
        log.info("Body:");
        log.info("Hello,");
        log.info("");
        log.info("You have requested to reset your password. Please click the link below to reset your password:");
        log.info(resetLink);
        log.info("");
        log.info("This link will expire in 1 hour.");
        log.info("");
        log.info("If you did not request this password reset, please ignore this email.");
        log.info("");
        log.info("Best regards,");
        log.info("Personal Hub Team");
        log.info("================================");
    }
}