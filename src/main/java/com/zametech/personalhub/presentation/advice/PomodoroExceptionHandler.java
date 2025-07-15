package com.zametech.personalhub.presentation.advice;

import com.zametech.personalhub.domain.exception.ActiveSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class PomodoroExceptionHandler {
    
    @ExceptionHandler(ActiveSessionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleActiveSessionNotFound(ActiveSessionNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "NOT_FOUND");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}