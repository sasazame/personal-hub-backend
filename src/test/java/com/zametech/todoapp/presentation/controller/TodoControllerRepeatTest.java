package com.zametech.todoapp.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.TodoService;
import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.RepeatConfigRequest;
import com.zametech.todoapp.presentation.dto.response.RepeatConfigResponse;
import com.zametech.todoapp.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TodoController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class TodoControllerRepeatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    @Test
    @WithMockUser
    void testCreateRepeatableTodo() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Daily Task",
            "Daily task description",
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        RepeatConfigResponse repeatConfigResponse = new RepeatConfigResponse(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        TodoResponse response = new TodoResponse(
            1L,
            "Daily Task",
            "Daily task description",
            TodoStatus.TODO,
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfigResponse,
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.createTodo(any(CreateTodoRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Daily Task"))
                .andExpect(jsonPath("$.isRepeatable").value(true))
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("DAILY"))
                .andExpect(jsonPath("$.repeatConfig.interval").value(1));
    }

    @Test
    @WithMockUser
    void testCreateRepeatableTodo_Weekly() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.WEEKLY,
            1,
            List.of(1, 5), // Monday and Friday
            null,
            null
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Weekly Task",
            "Weekly task description",
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 6), // Monday
            null,
            true,
            repeatConfig
        );

        RepeatConfigResponse repeatConfigResponse = new RepeatConfigResponse(
            RepeatType.WEEKLY,
            1,
            List.of(1, 5),
            null,
            null
        );

        TodoResponse response = new TodoResponse(
            1L,
            "Weekly Task",
            "Weekly task description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 6),
            null,
            true,
            repeatConfigResponse,
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.createTodo(any(CreateTodoRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("WEEKLY"))
                .andExpect(jsonPath("$.repeatConfig.daysOfWeek[0]").value(1))
                .andExpect(jsonPath("$.repeatConfig.daysOfWeek[1]").value(5));
    }

    @Test
    @WithMockUser
    void testGetRepeatableTodos() throws Exception {
        // Given
        RepeatConfigResponse repeatConfigResponse = new RepeatConfigResponse(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        TodoResponse todo1 = new TodoResponse(
            1L,
            "Daily Task 1",
            "Description 1",
            TodoStatus.TODO,
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfigResponse,
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        TodoResponse todo2 = new TodoResponse(
            2L,
            "Daily Task 2",
            "Description 2",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 2),
            null,
            true,
            repeatConfigResponse,
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.getRepeatableTodos()).thenReturn(List.of(todo1, todo2));

        // When & Then
        mockMvc.perform(get("/api/v1/todos/repeatable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].isRepeatable").value(true))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].isRepeatable").value(true));
    }

    @Test
    @WithMockUser
    void testGetRepeatInstances() throws Exception {
        // Given
        Long originalTodoId = 1L;

        TodoResponse instance1 = new TodoResponse(
            2L,
            "Instance 1",
            "Generated instance 1",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 2),
            null,
            false,
            null,
            originalTodoId,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        TodoResponse instance2 = new TodoResponse(
            3L,
            "Instance 2",
            "Generated instance 2",
            TodoStatus.DONE,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 3),
            null,
            false,
            null,
            originalTodoId,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.getRepeatInstances(originalTodoId)).thenReturn(List.of(instance1, instance2));

        // When & Then
        mockMvc.perform(get("/api/v1/todos/{originalTodoId}/instances", originalTodoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].originalTodoId").value(originalTodoId))
                .andExpect(jsonPath("$[0].isRepeatable").value(false))
                .andExpect(jsonPath("$[1].id").value(3L))
                .andExpect(jsonPath("$[1].originalTodoId").value(originalTodoId))
                .andExpect(jsonPath("$[1].status").value("DONE"));
    }

    @Test
    @WithMockUser
    void testGeneratePendingRepeatInstances() throws Exception {
        // Given
        TodoResponse generatedInstance = new TodoResponse(
            5L,
            "Generated Task",
            "Auto-generated instance",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 5),
            null,
            false,
            null,
            1L,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.generatePendingRepeatInstances()).thenReturn(List.of(generatedInstance));

        // When & Then
        mockMvc.perform(post("/api/v1/todos/repeat/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].title").value("Generated Task"))
                .andExpect(jsonPath("$[0].originalTodoId").value(1L))
                .andExpect(jsonPath("$[0].isRepeatable").value(false));
    }

    @Test
    @WithMockUser
    void testCreateRepeatableTodo_ValidationError() throws Exception {
        // Given - 繰り返し設定が有効だが、repeatConfigがnull - バリデーションエラーはJSONマッピング時に発生

        String invalidRequestJson = """
            {
                "title": "Invalid Task",
                "description": "Task without repeat config",
                "priority": "MEDIUM",
                "dueDate": "2025-01-01",
                "parentId": null,
                "isRepeatable": true,
                "repeatConfig": null
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testCreateRepeatableTodo_MonthlyWithDayOfMonth() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.MONTHLY,
            1,
            null,
            15, // 毎月15日
            LocalDate.of(2025, 12, 31) // 終了日
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Monthly Task",
            "Monthly on 15th",
            TodoPriority.LOW,
            LocalDate.of(2025, 1, 15),
            null,
            true,
            repeatConfig
        );

        RepeatConfigResponse repeatConfigResponse = new RepeatConfigResponse(
            RepeatType.MONTHLY,
            1,
            null,
            15,
            LocalDate.of(2025, 12, 31)
        );

        TodoResponse response = new TodoResponse(
            1L,
            "Monthly Task",
            "Monthly on 15th",
            TodoStatus.TODO,
            TodoPriority.LOW,
            LocalDate.of(2025, 1, 15),
            null,
            true,
            repeatConfigResponse,
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        when(todoService.createTodo(any(CreateTodoRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("MONTHLY"))
                .andExpect(jsonPath("$.repeatConfig.dayOfMonth").value(15))
                .andExpect(jsonPath("$.repeatConfig.endDate").value("2025-12-31"));
    }
}