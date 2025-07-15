package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a Pomodoro session.
 */
public class UpdatePomodoroSessionRequest {
    @NotNull(message = "Action is required")
    private SessionAction action;
    
    private SessionType sessionType;

    public enum SessionAction {
        START,
        PAUSE,
        RESUME,
        COMPLETE,
        CANCEL,
        SWITCH_TYPE
    }

    public enum SessionType {
        WORK,
        SHORT_BREAK,
        LONG_BREAK
    }

    // Getters and Setters
    public SessionAction getAction() {
        return action;
    }

    public void setAction(SessionAction action) {
        this.action = action;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }
}