package com.zametech.todoapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.AuthenticationService;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.presentation.dto.request.CreateEventRequest;
import com.zametech.todoapp.presentation.dto.request.CreateNoteRequest;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        try {
            // Create test user for integration tests
            RegisterRequest registerRequest = new RegisterRequest(
                    "test@example.com",
                    "TestPassword123!",
                    "testuser"
            );
            authenticationService.register(registerRequest);
        } catch (Exception e) {
            // User already exists, ignore
        }
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getDashboard_Success() throws Exception {
        // Create some test data
        createTestTodo();
        createTestEvent();
        createTestNote();

        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todoStats").exists())
                .andExpect(jsonPath("$.todoStats.totalTodos").isNumber())
                .andExpect(jsonPath("$.eventStats").exists())
                .andExpect(jsonPath("$.eventStats.totalEvents").isNumber())
                .andExpect(jsonPath("$.noteStats").exists())
                .andExpect(jsonPath("$.noteStats.totalNotes").isNumber())
                .andExpect(jsonPath("$.productivityStats").exists());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getTodoActivity_Success() throws Exception {
        // Create some test data
        createTestTodo();

        mockMvc.perform(get("/api/v1/analytics/todos/activity"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dailyCompletions").exists())
                .andExpect(jsonPath("$.dailyCreations").exists())
                .andExpect(jsonPath("$.priorityDistribution").exists())
                .andExpect(jsonPath("$.statusDistribution").exists())
                .andExpect(jsonPath("$.averageCompletionTimeInDays").isNumber());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getDashboard_EmptyData() throws Exception {
        // Test with no data
        mockMvc.perform(get("/api/v1/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.todoStats.totalTodos").value(0))
                .andExpect(jsonPath("$.eventStats.totalEvents").value(0))
                .andExpect(jsonPath("$.noteStats.totalNotes").value(0));
    }

    private void createTestTodo() throws Exception {
        CreateTodoRequest request = new CreateTodoRequest(
                "Test Todo",
                "Test Description",
                TodoPriority.MEDIUM,
                LocalDate.now().plusDays(1),
                null,
                false,
                null
        );

        mockMvc.perform(post("/api/v1/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void createTestEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                "Test Location",
                false,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void createTestNote() throws Exception {
        CreateNoteRequest request = new CreateNoteRequest(
                "Test Note",
                "Test Content",
                List.of("test")
        );

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}