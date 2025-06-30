package com.zametech.todoapp.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MockEmailServiceTest {

    @InjectMocks
    private MockEmailService mockEmailService;

    private ListAppender<ILoggingEvent> logWatcher;
    private Logger logger;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mockEmailService, "frontendUrl", "http://localhost:3000");
        
        logger = (Logger) LoggerFactory.getLogger(MockEmailService.class);
        logWatcher = new ListAppender<>();
        logWatcher.start();
        logger.addAppender(logWatcher);
    }

    @Test
    void sendPasswordResetEmail_shouldLogEmailDetails() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        String expectedResetLink = "http://localhost:3000/reset-password?token=test-reset-token-123";

        // When
        mockEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        List<ILoggingEvent> logsList = logWatcher.list;
        assertThat(logsList).hasSizeGreaterThan(0);
        
        // Verify email header log
        assertThat(logsList).anyMatch(event -> 
            event.getLevel() == Level.INFO && 
            event.getFormattedMessage().equals("=== PASSWORD RESET EMAIL (MOCK) ===")
        );
        
        // Verify recipient log
        assertThat(logsList).anyMatch(event -> 
            event.getLevel() == Level.INFO && 
            event.getFormattedMessage().equals("To: " + toEmail)
        );
        
        // Verify reset link log
        assertThat(logsList).anyMatch(event -> 
            event.getLevel() == Level.INFO && 
            event.getFormattedMessage().equals(expectedResetLink)
        );
    }

    @Test
    void sendPasswordResetEmail_withCustomFrontendUrl_shouldLogCorrectLink() {
        // Given
        String customFrontendUrl = "https://app.example.com";
        ReflectionTestUtils.setField(mockEmailService, "frontendUrl", customFrontendUrl);
        
        String toEmail = "test@example.com";
        String resetToken = "custom-token-456";
        String expectedResetLink = customFrontendUrl + "/reset-password?token=" + resetToken;

        // When
        mockEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        List<ILoggingEvent> logsList = logWatcher.list;
        assertThat(logsList).anyMatch(event -> 
            event.getLevel() == Level.INFO && 
            event.getFormattedMessage().equals(expectedResetLink)
        );
    }

    @Test
    void sendPasswordResetEmail_shouldLogAllRequiredInformation() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-token";

        // When
        mockEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        List<ILoggingEvent> logsList = logWatcher.list;
        
        // Verify all required elements are logged
        assertThat(logsList).anyMatch(event -> event.getFormattedMessage().contains("Subject: Password Reset Request"));
        assertThat(logsList).anyMatch(event -> event.getFormattedMessage().contains("Hello,"));
        assertThat(logsList).anyMatch(event -> event.getFormattedMessage().contains("This link will expire in 1 hour."));
        assertThat(logsList).anyMatch(event -> event.getFormattedMessage().contains("Personal Hub Team"));
    }

    @Test
    void sendPasswordResetEmail_shouldNotThrowException() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token";

        // When & Then - should complete without throwing
        mockEmailService.sendPasswordResetEmail(toEmail, resetToken);
    }
}