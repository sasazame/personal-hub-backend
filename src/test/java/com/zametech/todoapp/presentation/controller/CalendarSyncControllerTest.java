package com.zametech.todoapp.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.CalendarSyncService;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import com.zametech.todoapp.presentation.dto.response.GoogleSyncSettingsResponse;
import com.zametech.todoapp.presentation.dto.response.GoogleSyncStatusResponse;
import com.zametech.todoapp.presentation.dto.request.GoogleSyncSettingsRequest;
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
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CalendarSyncController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
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
    
    @MockBean
    private UserContextService userContextService;

    @Test
    @WithMockUser(roles = "USER")
    void testGetSyncStatus_ReturnsStatusSuccessfully() throws Exception {
        // Given
        CalendarSyncStatusResponse.SyncStatistics stats = 
            new CalendarSyncStatusResponse.SyncStatistics(10, 8, 1, 1, LocalDateTime.now());
        
        CalendarSyncStatusResponse response = new CalendarSyncStatusResponse(
            true, LocalDateTime.now(), "SUCCESS", List.of(), stats
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(calendarSyncService.getSyncStatus()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isConnected").value(true))
                .andExpect(jsonPath("$.syncStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.syncStatistics.totalEvents").value(10))
                .andExpect(jsonPath("$.syncStatistics.syncedEvents").value(8))
                .andExpect(jsonPath("$.syncStatistics.pendingEvents").value(1))
                .andExpect(jsonPath("$.syncStatistics.errorEvents").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetSyncSettings_ReturnsEmptyList() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of());
        
        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testTestConnection_WithValidCredentials_ReturnsSuccess() throws Exception {
        // Given
        when(calendarSyncService.connectGoogleCalendar()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Connection successful. Found 0 calendars."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testTestConnection_WithInvalidCredentials_ReturnsBadRequest() throws Exception {
        // Given
        when(calendarSyncService.connectGoogleCalendar())
            .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Connection failed")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDisconnectGoogleCalendar_ReturnsNoContent() throws Exception {
        // Given
        String calendarId = "primary";

        // When & Then
        mockMvc.perform(delete("/api/v1/calendar/sync/disconnect/{calendarId}", calendarId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetAuthorizationUrl_ReturnsUrl() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/url"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://accounts.google.com")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testHandleOAuthCallback_ReturnsSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/callback")
                .param("code", "auth_code_123")
                .param("state", "state_123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authorization successful"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetGoogleSyncSettings_ReturnsSettings() throws Exception {
        // Given
        GoogleSyncSettingsResponse mockResponse = new GoogleSyncSettingsResponse(
            true, "primary", "BIDIRECTIONAL", true, 30
        );
        when(calendarSyncService.getGoogleSyncSettings()).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.calendarId").value("primary"))
                .andExpect(jsonPath("$.syncDirection").value("BIDIRECTIONAL"))
                .andExpect(jsonPath("$.autoSync").value(true))
                .andExpect(jsonPath("$.syncInterval").value(30));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateGoogleSyncSettings_ReturnsUpdatedSettings() throws Exception {
        // Given
        GoogleSyncSettingsRequest request = new GoogleSyncSettingsRequest(
            true, "calendar123", "TO_GOOGLE", false, 60
        );
        GoogleSyncSettingsResponse mockResponse = new GoogleSyncSettingsResponse(
            true, "calendar123", "TO_GOOGLE", false, 60
        );
        when(calendarSyncService.updateGoogleSyncSettings(any(GoogleSyncSettingsRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/calendar/sync/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.calendarId").value("calendar123"))
                .andExpect(jsonPath("$.syncDirection").value("TO_GOOGLE"))
                .andExpect(jsonPath("$.autoSync").value(false))
                .andExpect(jsonPath("$.syncInterval").value(60));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testTriggerManualSync_ReturnsStatus() throws Exception {
        // Given
        GoogleSyncStatusResponse mockResponse = new GoogleSyncStatusResponse(
            "2025-06-24T20:44:18", "2025-06-24T21:14:18", false, 5, List.of()
        );
        when(calendarSyncService.triggerManualSync()).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastSyncTime").value("2025-06-24T20:44:18"))
                .andExpect(jsonPath("$.nextSyncTime").value("2025-06-24T21:14:18"))
                .andExpect(jsonPath("$.isRunning").value(false))
                .andExpect(jsonPath("$.syncedEvents").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetGoogleSyncStatus_ReturnsStatus() throws Exception {
        // Given
        GoogleSyncStatusResponse mockResponse = new GoogleSyncStatusResponse(
            "2025-06-24T20:44:18", null, false, 10, List.of("Error syncing event A")
        );
        when(calendarSyncService.getGoogleSyncStatus()).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastSyncTime").value("2025-06-24T20:44:18"))
                .andExpect(jsonPath("$.nextSyncTime").isEmpty())
                .andExpect(jsonPath("$.isRunning").value(false))
                .andExpect(jsonPath("$.syncedEvents").value(10))
                .andExpect(jsonPath("$.errors[0]").value("Error syncing event A"));
    }
}