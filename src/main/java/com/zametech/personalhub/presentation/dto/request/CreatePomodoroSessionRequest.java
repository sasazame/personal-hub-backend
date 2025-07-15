package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating a new Pomodoro session.
 */
public class CreatePomodoroSessionRequest {
    @NotNull(message = "Work duration is required")
    @Min(value = 1, message = "Work duration must be at least 1 minute")
    @Max(value = 120, message = "Work duration cannot exceed 120 minutes")
    private Integer workDuration;

    @NotNull(message = "Break duration is required")
    @Min(value = 1, message = "Break duration must be at least 1 minute")
    @Max(value = 60, message = "Break duration cannot exceed 60 minutes")
    private Integer breakDuration;

    private List<CreatePomodoroTaskRequest> tasks;

    // Getters and Setters
    public Integer getWorkDuration() {
        return workDuration;
    }

    public void setWorkDuration(Integer workDuration) {
        this.workDuration = workDuration;
    }

    public Integer getBreakDuration() {
        return breakDuration;
    }

    public void setBreakDuration(Integer breakDuration) {
        this.breakDuration = breakDuration;
    }

    public List<CreatePomodoroTaskRequest> getTasks() {
        return tasks;
    }

    public void setTasks(List<CreatePomodoroTaskRequest> tasks) {
        this.tasks = tasks;
    }
}