package com.zametech.todoapp.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevoEmailServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BrevoEmailService brevoEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(brevoEmailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(brevoEmailService, "apiUrl", "https://api.brevo.com/v3");
        ReflectionTestUtils.setField(brevoEmailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(brevoEmailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(brevoEmailService, "fromName", "Test App");
    }

    @Test
    void sendPasswordResetEmail_withSuccessfulResponse_shouldSendEmail() throws Exception {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        String jsonBody = "{\"test\":\"data\"}";
        String responseBody = "{\"messageId\":\"message-123\"}";
        
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonBody);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(
            eq("https://api.brevo.com/v3/smtp/email"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(successResponse);
        
        when(objectMapper.readValue(responseBody, Map.class))
            .thenReturn(Map.of("messageId", "message-123"));

        // When
        brevoEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        ArgumentCaptor<HttpEntity<String>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            eq("https://api.brevo.com/v3/smtp/email"),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            eq(String.class)
        );
        
        HttpEntity<String> capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeaders().get("api-key")).contains("test-api-key");
        assertThat(capturedRequest.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void sendPasswordResetEmail_withFailureResponse_shouldThrowException() throws Exception {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        String jsonBody = "{\"test\":\"data\"}";
        
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonBody);
        
        ResponseEntity<String> failureResponse = new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(failureResponse);

        // When & Then
        assertThatThrownBy(() -> brevoEmailService.sendPasswordResetEmail(toEmail, resetToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to send password reset email");
    }

    @Test
    void sendPasswordResetEmail_withRestTemplateException_shouldThrowRuntimeException() throws Exception {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Network error"));

        // When & Then
        assertThatThrownBy(() -> brevoEmailService.sendPasswordResetEmail(toEmail, resetToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to send password reset email")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void sendPasswordResetEmail_shouldBuildCorrectEmailData() throws Exception {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        ArgumentCaptor<Object> emailDataCaptor = ArgumentCaptor.forClass(Object.class);
        when(objectMapper.writeValueAsString(emailDataCaptor.capture())).thenReturn("{}");
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(successResponse);

        // When
        brevoEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        Map<String, Object> capturedEmailData = (Map<String, Object>) emailDataCaptor.getValue();
        assertThat(capturedEmailData).containsKey("sender");
        assertThat(capturedEmailData).containsKey("to");
        assertThat(capturedEmailData).containsEntry("subject", "Password Reset Request - Personal Hub");
        assertThat(capturedEmailData).containsKey("htmlContent");
        
        Map<String, String> sender = (Map<String, String>) capturedEmailData.get("sender");
        assertThat(sender).containsEntry("email", "noreply@test.com");
        assertThat(sender).containsEntry("name", "Test App");
    }

    @Test
    void sendPasswordResetEmail_withCustomConfiguration_shouldUseCustomValues() throws Exception {
        // Given
        ReflectionTestUtils.setField(brevoEmailService, "frontendUrl", "https://app.example.com");
        ReflectionTestUtils.setField(brevoEmailService, "fromEmail", "support@example.com");
        ReflectionTestUtils.setField(brevoEmailService, "fromName", "Example Support");
        
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        
        ArgumentCaptor<Object> emailDataCaptor = ArgumentCaptor.forClass(Object.class);
        when(objectMapper.writeValueAsString(emailDataCaptor.capture())).thenReturn("{}");
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("{}", HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(successResponse);

        // When
        brevoEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then
        Map<String, Object> capturedEmailData = (Map<String, Object>) emailDataCaptor.getValue();
        Map<String, String> sender = (Map<String, String>) capturedEmailData.get("sender");
        assertThat(sender).containsEntry("email", "support@example.com");
        assertThat(sender).containsEntry("name", "Example Support");
        
        String htmlContent = (String) capturedEmailData.get("htmlContent");
        assertThat(htmlContent).contains("https://app.example.com/reset-password?token=" + resetToken);
    }

    @Test
    void sendPasswordResetEmail_withInvalidResponseBody_shouldStillSucceed() throws Exception {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token-123";
        String jsonBody = "{\"test\":\"data\"}";
        String invalidResponseBody = "not-json";
        
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonBody);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>(invalidResponseBody, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(successResponse);
        
        when(objectMapper.readValue(invalidResponseBody, Map.class))
            .thenThrow(new RuntimeException("Invalid JSON"));

        // When
        brevoEmailService.sendPasswordResetEmail(toEmail, resetToken);

        // Then - should complete without throwing
        verify(restTemplate).exchange(
            eq("https://api.brevo.com/v3/smtp/email"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
}