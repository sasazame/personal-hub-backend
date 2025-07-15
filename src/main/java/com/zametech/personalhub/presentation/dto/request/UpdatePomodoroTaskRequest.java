package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a Pomodoro task.
 */
public class UpdatePomodoroTaskRequest {
    @NotNull(message = "Completed status is required")
    private Boolean completed;

    // Getters and Setters
    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}