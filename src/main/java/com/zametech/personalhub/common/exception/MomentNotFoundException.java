package com.zametech.personalhub.common.exception;

/**
 * Exception thrown when a Moment is not found
 */
public class MomentNotFoundException extends RuntimeException {
    
    public MomentNotFoundException(Long id) {
        super("Moment not found with id: " + id);
    }
    
    public MomentNotFoundException(String message) {
        super(message);
    }
}