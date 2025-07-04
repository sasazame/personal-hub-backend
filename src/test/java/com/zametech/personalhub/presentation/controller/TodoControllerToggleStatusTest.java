package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.TodoService;
import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TodoController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class TodoControllerToggleStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    @Test
    @WithMockUser
    void testToggleTodoStatus_Success() throws Exception {
        // Given
        Long todoId = 1L;
        TodoResponse response = new TodoResponse(
                todoId,
                "Test TODO",
                "Test description",
                TodoStatus.DONE,
                TodoPriority.MEDIUM,
                null,
                null,
                false,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );

        when(todoService.toggleTodoStatus(todoId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").value("Test TODO"));
    }

    @Test
    @WithMockUser
    void testToggleTodoStatus_TodoNotFound() throws Exception {
        // Given
        Long todoId = 999L;
        when(todoService.toggleTodoStatus(todoId))
                .thenThrow(new TodoNotFoundException(todoId));

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("TODO not found with id: " + todoId));
    }

    @Test
    @WithMockUser
    void testToggleTodoStatus_AccessDenied() throws Exception {
        // Given
        Long todoId = 1L;
        when(todoService.toggleTodoStatus(todoId))
                .thenThrow(new AccessDeniedException("Access denied to TODO with id: " + todoId));

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("アクセス権限がありません"));
    }

    @Test
    @WithMockUser
    void testToggleTodoStatus_StatusToggleFromTodoToDone() throws Exception {
        // Given
        Long todoId = 1L;
        TodoResponse response = new TodoResponse(
                todoId,
                "Test TODO",
                "Test description",
                TodoStatus.DONE, // Toggled from TODO to DONE
                TodoPriority.HIGH,
                null,
                null,
                false,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );

        when(todoService.toggleTodoStatus(todoId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @WithMockUser
    void testToggleTodoStatus_StatusToggleFromDoneToTodo() throws Exception {
        // Given
        Long todoId = 1L;
        TodoResponse response = new TodoResponse(
                todoId,
                "Test TODO",
                "Test description",
                TodoStatus.TODO, // Toggled from DONE to TODO
                TodoPriority.LOW,
                null,
                null,
                false,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );

        when(todoService.toggleTodoStatus(todoId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("LOW"));
    }

    @Test
    @WithMockUser
    void testToggleTodoStatus_StatusToggleFromInProgressToDone() throws Exception {
        // Given
        Long todoId = 1L;
        TodoResponse response = new TodoResponse(
                todoId,
                "Test TODO",
                "Test description",
                TodoStatus.DONE, // Toggled from IN_PROGRESS to DONE
                TodoPriority.MEDIUM,
                null,
                null,
                false,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );

        when(todoService.toggleTodoStatus(todoId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void testToggleTodoStatus_WithoutAuthentication() throws Exception {
        // Given
        Long todoId = 1L;
        TodoResponse response = new TodoResponse(
                todoId,
                "Test TODO",
                "Test description",
                TodoStatus.DONE,
                TodoPriority.MEDIUM,
                null,
                null,
                false,
                null,
                null,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );
        
        when(todoService.toggleTodoStatus(todoId)).thenReturn(response);

        // When & Then - TestSecurityConfig makes all endpoints public
        mockMvc.perform(post("/api/v1/todos/{id}/toggle-status", todoId))
                .andExpect(status().isOk());
    }
}