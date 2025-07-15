package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a Pomodoro task.
 */
public class CreatePomodoroTaskRequest {
    private Long todoId;

    @NotBlank(message = "Task description is required")
    @Size(max = 500, message = "Task description cannot exceed 500 characters")
    private String description;

    // Getters and Setters
    public Long getTodoId() {
        return todoId;
    }

    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}