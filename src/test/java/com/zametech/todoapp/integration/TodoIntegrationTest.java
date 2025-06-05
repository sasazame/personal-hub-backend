package com.zametech.todoapp.integration;

import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.LoginRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateTodoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TODOエンドポイントの統合テスト（認証込み）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private String anotherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create first user and get token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setUsername("testuser");
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("accessToken").asText();

        // Create another user for access control tests
        RegisterRequest anotherUserRequest = new RegisterRequest();
        anotherUserRequest.setEmail("another@example.com");
        anotherUserRequest.setPassword("Password123!");
        anotherUserRequest.setUsername("anotheruser");
        
        MvcResult anotherResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherUserRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String anotherResponse = anotherResult.getResponse().getContentAsString();
        anotherUserToken = objectMapper.readTree(anotherResponse).get("accessToken").asText();
    }

    @Test
    void shouldRequireAuthenticationForTodoEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/todos"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/todos/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/todos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/todos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateTodoWithAuthentication() throws Exception {
        CreateTodoRequest createRequest = new CreateTodoRequest(
            "Test Todo",
            "Test Description",
            TodoPriority.HIGH,
            LocalDate.now().plusDays(7),
            null
        );

        mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Todo"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void shouldGetOnlyOwnTodos() throws Exception {
        // Create todo for first user
        CreateTodoRequest createRequest = new CreateTodoRequest(
            "User1 Todo",
            "Description",
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(1),
            null
        );

        mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Create todo for second user
        CreateTodoRequest anotherRequest = new CreateTodoRequest(
            "User2 Todo",
            "Another Description",
            TodoPriority.LOW,
            LocalDate.now().plusDays(2),
            null
        );

        mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + anotherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherRequest)))
                .andExpect(status().isCreated());

        // First user should only see their todo
        mockMvc.perform(get("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("User1 Todo"));

        // Second user should only see their todo
        mockMvc.perform(get("/api/v1/todos")
                .header("Authorization", "Bearer " + anotherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("User2 Todo"));
    }

    @Test
    void shouldPreventAccessToOtherUsersTodo() throws Exception {
        // Create todo for first user
        CreateTodoRequest createRequest = new CreateTodoRequest(
            "Private Todo",
            "Private Description",
            TodoPriority.HIGH,
            LocalDate.now(),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long todoId = objectMapper.readTree(response).get("id").asLong();

        // Second user should get 403 when accessing first user's todo
        mockMvc.perform(get("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + anotherUserToken))
                .andDo(print())
                .andExpect(status().isForbidden());

        // Second user should get 403 when updating first user's todo
        UpdateTodoRequest updateRequest = new UpdateTodoRequest(
            "Hacked Title",
            "Hacked Description",
            TodoStatus.DONE,
            TodoPriority.LOW,
            LocalDate.now(),
            null
        );

        mockMvc.perform(put("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + anotherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // Second user should get 403 when deleting first user's todo
        mockMvc.perform(delete("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + anotherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateOwnTodo() throws Exception {
        // Create todo
        CreateTodoRequest createRequest = new CreateTodoRequest(
            "Original Title",
            "Original Description",
            TodoPriority.LOW,
            LocalDate.now().plusDays(3),
            null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Long todoId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update todo
        UpdateTodoRequest updateRequest = new UpdateTodoRequest(
            "Updated Title",
            "Updated Description",
            TodoStatus.IN_PROGRESS,
            TodoPriority.HIGH,
            LocalDate.now().plusDays(5),
            null
        );

        mockMvc.perform(put("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void shouldDeleteOwnTodo() throws Exception {
        // Create todo
        CreateTodoRequest createRequest = new CreateTodoRequest(
            "To Be Deleted",
            "This will be deleted",
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Long todoId = objectMapper.readTree(createResponse).get("id").asLong();

        // Delete todo
        mockMvc.perform(delete("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + authToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetTodosByStatus() throws Exception {
        // Create todos with different statuses
        CreateTodoRequest todo1 = new CreateTodoRequest(
            "Todo 1",
            "Description 1",
            TodoPriority.HIGH,
            LocalDate.now(),
            null
        );

        CreateTodoRequest todo2 = new CreateTodoRequest(
            "Todo 2",
            "Description 2",
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );

        // Create first todo
        mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todo1)))
                .andExpect(status().isCreated());

        // Create second todo
        MvcResult result = mockMvc.perform(post("/api/v1/todos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todo2)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long todoId = objectMapper.readTree(response).get("id").asLong();

        // Update second todo to DONE
        UpdateTodoRequest updateRequest = new UpdateTodoRequest(
            "Todo 2",
            "Description 2",
            TodoStatus.DONE,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );

        mockMvc.perform(put("/api/v1/todos/" + todoId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Get only TODO status
        mockMvc.perform(get("/api/v1/todos")
                .param("status", "TODO")
                .header("Authorization", "Bearer " + authToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Todo 1"));

        // Get only DONE status
        mockMvc.perform(get("/api/v1/todos")
                .param("status", "DONE")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Todo 2"));
    }

    @Test
    void shouldHandleCorsForTodoEndpoints() throws Exception {
        mockMvc.perform(options("/api/v1/todos")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
}