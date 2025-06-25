package com.zametech.todoapp.common.exception;

/**
 * Exception thrown when an OAuth authorization code is used multiple times
 */
public class DuplicateAuthorizationCodeException extends RuntimeException {
    
    public DuplicateAuthorizationCodeException(String message) {
        super(message);
    }
    
    public DuplicateAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}