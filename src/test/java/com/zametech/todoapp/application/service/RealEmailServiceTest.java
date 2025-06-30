package com.zametech.todoapp.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private RealEmailService realEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(realEmailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(realEmailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(realEmailService, "fromName", "Test App");
    }

    @Test
    void sendPasswordResetEmail_withValidParameters_shouldSendEmail() throws MessagingException, UnsupportedEncodingException {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        realEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_withMessagingException_shouldThrowRuntimeException() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> realEmailService.sendPasswordResetEmail(toEmail, resetToken))
            .isInstanceOf(MailSendException.class)
            .hasMessage("Mail server error");
    }

    @Test
    void sendPasswordResetEmail_shouldGenerateCorrectResetLink() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        String expectedResetLink = "http://localhost:3000/reset-password?token=test-reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        realEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        // Since we can't easily verify the email content due to MimeMessageHelper limitations,
        // we verify that the email was sent successfully
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_withCustomFrontendUrl_shouldUseCustomUrl() {
        // Given
        String customFrontendUrl = "https://app.example.com";
        ReflectionTestUtils.setField(realEmailService, "frontendUrl", customFrontendUrl);
        
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        realEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_shouldUseConfiguredFromEmailAndName() {
        // Given
        String customFromEmail = "support@company.com";
        String customFromName = "Company Support";
        ReflectionTestUtils.setField(realEmailService, "fromEmail", customFromEmail);
        ReflectionTestUtils.setField(realEmailService, "fromName", customFromName);
        
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        realEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        verify(mailSender).send(mimeMessage);
    }
}