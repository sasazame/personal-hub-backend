package com.zametech.personalhub.presentation.dto.request;

import com.zametech.personalhub.presentation.validation.ValidSessionTypeForAction;
import com.zametech.personalhub.shared.constants.SessionAction;
import com.zametech.personalhub.shared.constants.SessionType;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a Pomodoro session.
 */
@ValidSessionTypeForAction
public class UpdatePomodoroSessionRequest {
    @NotNull(message = "Action is required")
    private SessionAction action;
    
    private SessionType sessionType;

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