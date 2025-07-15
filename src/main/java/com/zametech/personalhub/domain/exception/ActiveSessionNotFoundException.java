package com.zametech.personalhub.domain.exception;

public class ActiveSessionNotFoundException extends RuntimeException {
    public ActiveSessionNotFoundException() {
        super("No active Pomodoro session found");
    }
    
    public ActiveSessionNotFoundException(String message) {
        super(message);
    }
}