package com.zametech.todoapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.AuthenticationService;
import com.zametech.todoapp.presentation.dto.request.CreateEventRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateEventRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventIntegrationTest {

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
    void createEvent_Success() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Team Meeting",
                "Weekly team sync meeting",
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 11, 0),
                "Conference Room A",
                false,
                15,
                "#FF5722"
        );

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Team Meeting"))
                .andExpect(jsonPath("$.description").value("Weekly team sync meeting"))
                .andExpect(jsonPath("$.location").value("Conference Room A"))
                .andExpect(jsonPath("$.allDay").value(false))
                .andExpect(jsonPath("$.reminderMinutes").value(15))
                .andExpect(jsonPath("$.color").value("#FF5722"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getEvents_Success() throws Exception {
        // First create an event
        CreateEventRequest createRequest = new CreateEventRequest(
                "Test Event",
                "Test Description",
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 11, 0),
                null,
                false,
                null,
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long eventId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then get the event
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateEvent_Success() throws Exception {
        // First create an event
        CreateEventRequest createRequest = new CreateEventRequest(
                "Original Title",
                "Original Description",
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 11, 0),
                null,
                false,
                null,
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long eventId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then update the event
        UpdateEventRequest updateRequest = new UpdateEventRequest(
                "Updated Title",
                "Updated Description",
                null,
                null,
                "New Location",
                true,
                30,
                "#4CAF50"
        );

        mockMvc.perform(put("/api/v1/events/{id}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.location").value("New Location"))
                .andExpect(jsonPath("$.allDay").value(true))
                .andExpect(jsonPath("$.reminderMinutes").value(30))
                .andExpect(jsonPath("$.color").value("#4CAF50"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteEvent_Success() throws Exception {
        // First create an event
        CreateEventRequest createRequest = new CreateEventRequest(
                "Event to Delete",
                "This event will be deleted",
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 1, 15, 11, 0),
                null,
                false,
                null,
                null
        );

        String createResponse = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long eventId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then delete the event
        mockMvc.perform(delete("/api/v1/events/{id}", eventId))
                .andExpect(status().isNoContent());

        // Verify the event is deleted
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createEvent_InvalidData() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "", // Empty title
                null,
                null, // Missing start date
                null, // Missing end date
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}