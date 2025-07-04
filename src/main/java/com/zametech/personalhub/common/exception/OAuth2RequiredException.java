package com.zametech.personalhub.common.exception;

/**
 * Exception thrown when OAuth2 authentication is required for an operation
 */
public class OAuth2RequiredException extends RuntimeException {
    
    public OAuth2RequiredException(String message) {
        super(message);
    }
    
    public OAuth2RequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}