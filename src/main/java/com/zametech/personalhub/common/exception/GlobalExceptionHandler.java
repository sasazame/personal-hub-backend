package com.zametech.personalhub.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * グローバル例外ハンドラー
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * TODOが見つからない場合
     */
    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTodoNotFoundException(TodoNotFoundException e) {
        log.warn("TODO not found: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TODO_NOT_FOUND",
            e.getMessage(),
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Momentが見つからない場合
     */
    @ExceptionHandler(MomentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMomentNotFoundException(MomentNotFoundException e) {
        log.warn("Moment not found: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MOMENT_NOT_FOUND",
            e.getMessage(),
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * バリデーションエラー
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "入力値が不正です",
            errors,
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * JSON デシリアライゼーション エラー（DTOコンストラクタ内でのバリデーション失敗を含む）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HTTP message not readable: {}", e.getMessage());
        log.error("Root cause: ", e.getRootCause());
        log.error("Full exception: ", e);
        
        // DTOコンストラクタでのIllegalArgumentExceptionかチェック
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof IllegalArgumentException) {
            ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                rootCause.getMessage(),
                ZonedDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        // その他のJSON parsing errors
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_JSON",
            "リクエストの形式が不正です",
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * パスパラメータの型変換エラー
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch error: {}", e.getMessage());
        
        String message = String.format("パラメータ '%s' の値 '%s' は不正です", 
            e.getName(), e.getValue());
            
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_PARAMETER",
            message,
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 認証失敗（パスワード間違いなど）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "AUTHENTICATION_FAILED",
            "認証に失敗しました",
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * アクセス権限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "アクセス権限がありません",
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * 引数不正エラー（DTOコンストラクタ内でのバリデーション失敗）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument error: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            e.getMessage(),
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * OAuth2 required exceptions
     */
    @ExceptionHandler(OAuth2RequiredException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2RequiredException(OAuth2RequiredException ex) {
        log.warn("OAuth2 authentication required: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
                "OAUTH2_REQUIRED",
                ex.getMessage(),
                ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * Duplicate authorization code exceptions
     */
    @ExceptionHandler(DuplicateAuthorizationCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAuthorizationCodeException(DuplicateAuthorizationCodeException ex) {
        log.warn("Duplicate authorization code used: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
                "DUPLICATE_AUTH_CODE",
                "The authorization code has already been used. Please try logging in again.",
                ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Token decryption exceptions
     */
    @ExceptionHandler(TokenDecryptionException.class)
    public ResponseEntity<ErrorResponse> handleTokenDecryptionException(TokenDecryptionException ex) {
        log.warn("Token decryption failed: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
                "TOKEN_DECRYPTION_FAILED",
                "Your authentication tokens could not be decrypted. Please re-authenticate with Google.",
                ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 重複ユーザー登録エラー
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        if (e.getMessage() != null && e.getMessage().contains("Email already exists")) {
            log.warn("Duplicate email registration attempt: {}", e.getMessage());
            
            ErrorResponse errorResponse = new ErrorResponse(
                "EMAIL_ALREADY_EXISTS",
                "このメールアドレスは既に使用されています",
                ZonedDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
        
        // その他のRuntimeExceptionは500エラーとして処理
        log.error("Unexpected runtime error occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "サーバーエラーが発生しました",
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * その他の予期しないエラー
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "サーバーエラーが発生しました",
            ZonedDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * エラーレスポンス
     */
    public record ErrorResponse(
        String code,
        String message,
        Map<String, String> details,
        ZonedDateTime timestamp
    ) {
        public ErrorResponse(String code, String message, ZonedDateTime timestamp) {
            this(code, message, null, timestamp);
        }
    }
}