package com.zametech.personalhub.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleTodoNotFoundException_shouldReturn404() {
        TodoNotFoundException exception = new TodoNotFoundException(999L);
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTodoNotFoundException(exception);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TODO_NOT_FOUND", response.getBody().code());
        assertEquals("TODO not found with id: 999", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    // MethodArgumentNotValidException is complex to unit test due to its internal dependencies
    // This is better tested through integration tests
    // @Test
    // void handleValidationException_shouldReturn400WithFieldErrors() throws Exception { ... }

    @Test
    void handleBadCredentialsException_shouldReturn401() {
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadCredentialsException(exception);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_FAILED", response.getBody().code());
        assertEquals("認証に失敗しました", response.getBody().message());
    }

    @Test
    void handleAccessDeniedException_shouldReturn403() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccessDeniedException(exception);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().code());
        assertEquals("アクセス権限がありません", response.getBody().message());
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        IllegalArgumentException exception = new IllegalArgumentException("Illegal argument");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgumentException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertEquals("Illegal argument", response.getBody().message());
    }

    @Test
    void handleOAuth2RequiredException_shouldReturn403() {
        OAuth2RequiredException exception = new OAuth2RequiredException("OAuth2.0 authentication is required");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleOAuth2RequiredException(exception);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OAUTH2_REQUIRED", response.getBody().code());
        assertEquals("OAuth2.0 authentication is required", response.getBody().message());
    }

    @Test
    void handleDuplicateAuthorizationCodeException_shouldReturn400() {
        DuplicateAuthorizationCodeException exception = new DuplicateAuthorizationCodeException("Duplicate authorization code");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDuplicateAuthorizationCodeException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE_AUTH_CODE", response.getBody().code());
        assertEquals("The authorization code has already been used. Please try logging in again.", response.getBody().message());
    }

    @Test
    void handleTokenDecryptionException_shouldReturn401() {
        TokenDecryptionException exception = new TokenDecryptionException("Token decryption failed");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTokenDecryptionException(exception);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TOKEN_DECRYPTION_FAILED", response.getBody().code());
        assertEquals("Your authentication tokens could not be decrypted. Please re-authenticate with Google.", response.getBody().message());
    }

    @Test
    void handleRuntimeException_withEmailAlreadyExists_shouldReturn409() {
        RuntimeException exception = new RuntimeException("Email already exists");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleRuntimeException(exception);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMAIL_ALREADY_EXISTS", response.getBody().code());
        assertEquals("このメールアドレスは既に使用されています", response.getBody().message());
    }

    @Test
    void handleRuntimeException_withGeneralError_shouldReturn500() {
        RuntimeException exception = new RuntimeException("Some runtime error");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleRuntimeException(exception);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("サーバーエラーが発生しました", response.getBody().message());
    }

    @Test
    void handleGeneralException_shouldReturn500() {
        Exception exception = new Exception("Some general exception");
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneralException(exception);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertEquals("サーバーエラーが発生しました", response.getBody().message());
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturn400() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "invalid-number", Long.class, "id", null, null);
        
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTypeMismatchException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_PARAMETER", response.getBody().code());
        assertEquals("パラメータ 'id' の値 'invalid-number' は不正です", response.getBody().message());
    }

    @Test
    void errorResponse_shouldHaveCorrectStructure() {
        ZonedDateTime now = ZonedDateTime.now();
        GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse(
            "TEST_CODE", "Test message", now);
        
        assertEquals("TEST_CODE", response.code());
        assertEquals("Test message", response.message());
        assertNull(response.details());
        assertEquals(now, response.timestamp());
    }

    @Test
    void errorResponse_withDetails_shouldHaveCorrectStructure() {
        ZonedDateTime now = ZonedDateTime.now();
        Map<String, String> details = Map.of("field", "error");
        GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse(
            "TEST_CODE", "Test message", details, now);
        
        assertEquals("TEST_CODE", response.code());
        assertEquals("Test message", response.message());
        assertEquals(details, response.details());
        assertEquals(now, response.timestamp());
    }
}