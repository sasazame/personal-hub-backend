package com.zametech.personalhub.common.exception;

/**
 * TODOが見つからない場合の例外
 */
public class TodoNotFoundException extends RuntimeException {
    
    public TodoNotFoundException(Long id) {
        super("TODO not found with id: " + id);
    }
    
    public TodoNotFoundException(String message) {
        super(message);
    }
}