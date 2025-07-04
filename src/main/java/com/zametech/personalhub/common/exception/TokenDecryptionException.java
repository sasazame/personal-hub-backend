package com.zametech.personalhub.common.exception;

/**
 * Exception thrown when token decryption fails
 */
public class TokenDecryptionException extends RuntimeException {
    
    public TokenDecryptionException(String message) {
        super(message);
    }
    
    public TokenDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}