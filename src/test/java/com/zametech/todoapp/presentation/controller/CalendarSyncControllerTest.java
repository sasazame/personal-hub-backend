package com.zametech.todoapp.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.CalendarSyncService;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalendarSyncController.class)
@Import(TestSecurityConfig.class)
class CalendarSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendarSyncService calendarSyncService;

    @MockBean
    private CalendarSyncSettingsRepository calendarSyncSettingsRepository;

    @Test
    @WithMockUser
    void testGetSyncStatus_ReturnsStatusSuccessfully() throws Exception {
        // Given
        CalendarSyncStatusResponse.SyncStatistics stats = 
            new CalendarSyncStatusResponse.SyncStatistics(10, 8, 1, 1, LocalDateTime.now());
        
        CalendarSyncStatusResponse response = new CalendarSyncStatusResponse(
            true, LocalDateTime.now(), "SUCCESS", List.of(), stats
        );
        
        when(calendarSyncService.getSyncStatus()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.syncStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.syncStatistics.totalEvents").value(10))
                .andExpect(jsonPath("$.syncStatistics.syncedEvents").value(8))
                .andExpect(jsonPath("$.syncStatistics.pendingEvents").value(1))
                .andExpect(jsonPath("$.syncStatistics.errorEvents").value(1));
    }

    @Test
    @WithMockUser
    void testGetSyncSettings_ReturnsEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void testTestConnection_WithValidCredentials_ReturnsSuccess() throws Exception {
        // Given
        String credentials = "{\"type\":\"service_account\"}";
        when(calendarSyncService.connectGoogleCalendar(credentials)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isOk())
                .andExpect(content().string("Connection successful. Found 0 calendars."));
    }

    @Test
    @WithMockUser
    void testTestConnection_WithInvalidCredentials_ReturnsBadRequest() throws Exception {
        // Given
        String credentials = "{\"invalid\":\"credentials\"}";
        when(calendarSyncService.connectGoogleCalendar(credentials))
            .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Connection failed")));
    }

    @Test
    @WithMockUser
    void testDisconnectGoogleCalendar_ReturnsNoContent() throws Exception {
        // Given
        String calendarId = "primary";

        // When & Then
        mockMvc.perform(delete("/api/v1/calendar/sync/disconnect/{calendarId}", calendarId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void testGetAuthorizationUrl_ReturnsUrl() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/url"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://accounts.google.com")));
    }

    @Test
    @WithMockUser
    void testHandleOAuthCallback_ReturnsSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/callback")
                .param("code", "auth_code_123")
                .param("state", "state_123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authorization successful"));
    }
}