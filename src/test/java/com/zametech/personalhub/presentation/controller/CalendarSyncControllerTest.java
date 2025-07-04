package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.zametech.personalhub.application.service.CalendarSyncService;
import com.zametech.personalhub.application.service.UserContextService;
import com.zametech.personalhub.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.personalhub.presentation.dto.request.CalendarSyncSettingsRequest;
import com.zametech.personalhub.presentation.dto.request.GoogleSyncSettingsRequest;
import com.zametech.personalhub.presentation.dto.response.CalendarSyncStatusResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncSettingsResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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

    private UUID userId;
    private CalendarListEntry calendarEntry;
    private CalendarSyncSettingsEntity syncSettings;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        calendarEntry = new CalendarListEntry();
        calendarEntry.setId("primary");
        calendarEntry.setSummary("Primary Calendar");
        
        syncSettings = new CalendarSyncSettingsEntity(userId, "primary", "Primary Calendar");
        syncSettings.setId(1L);
        syncSettings.setSyncEnabled(true);
        syncSettings.setSyncDirection("BIDIRECTIONAL");
        syncSettings.setLastSyncAt(LocalDateTime.now());
        syncSettings.setCreatedAt(LocalDateTime.now());
        syncSettings.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "USER")
    void connectGoogleCalendar_Success() throws Exception {
        // Given
        List<CalendarListEntry> calendars = Arrays.asList(calendarEntry);
        when(calendarSyncService.connectGoogleCalendar()).thenReturn(calendars);

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("primary"))
                .andExpect(jsonPath("$[0].summary").value("Primary Calendar"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void connectGoogleCalendar_Error_ReturnsBadRequest() throws Exception {
        // Given
        when(calendarSyncService.connectGoogleCalendar()).thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/connect"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void disconnectGoogleCalendar_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/calendar/sync/disconnect/primary"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void disconnectGoogleCalendar_Error_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new RuntimeException("Disconnect failed")).when(calendarSyncService).disconnectGoogleCalendar(anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/calendar/sync/disconnect/primary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void performManualSync_Success() throws Exception {
        // Given
        CalendarSyncStatusResponse status = new CalendarSyncStatusResponse(
                true, 
                LocalDateTime.now(), 
                "SYNCED",
                List.of(),
                new CalendarSyncStatusResponse.SyncStatistics(10, 8, 0, 2, LocalDateTime.now())
        );
        when(calendarSyncService.performSync()).thenReturn(status);

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/manual/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isConnected").value(true))
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.syncStatistics.totalEvents").value(10))
                .andExpect(jsonPath("$.syncStatistics.syncedEvents").value(8));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSyncStatus_Success() throws Exception {
        // Given
        CalendarSyncStatusResponse status = new CalendarSyncStatusResponse(
                true, 
                LocalDateTime.now(), 
                "SYNCED",
                List.of(),
                new CalendarSyncStatusResponse.SyncStatistics(10, 8, 0, 2, LocalDateTime.now())
        );
        when(calendarSyncService.getSyncStatus()).thenReturn(status);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isConnected").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSyncSettings_Success() throws Exception {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(Arrays.asList(syncSettings));

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].googleCalendarId").value("primary"))
                .andExpect(jsonPath("$[0].calendarName").value("Primary Calendar"))
                .andExpect(jsonPath("$[0].syncEnabled").value(true))
                .andExpect(jsonPath("$[0].syncDirection").value("BIDIRECTIONAL"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateSyncSettings_ReturnsNotFound() throws Exception {
        // Given
        CalendarSyncSettingsRequest request = new CalendarSyncSettingsRequest(
                "primary",
                "Primary Calendar",
                true, 
                "BIDIRECTIONAL"
        );

        // When & Then
        mockMvc.perform(put("/api/v1/calendar/sync/settings/calendar/primary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAuthorizationUrl_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/url"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://accounts.google.com")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void handleOAuthCallback_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/auth/callback")
                        .param("code", "test-auth-code")
                        .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authorization successful"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testConnection_Success() throws Exception {
        // Given
        List<CalendarListEntry> calendars = Arrays.asList(calendarEntry, new CalendarListEntry());
        when(calendarSyncService.connectGoogleCalendar()).thenReturn(calendars);

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Connection successful. Found 2 calendars."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testConnection_Failure() throws Exception {
        // Given
        when(calendarSyncService.connectGoogleCalendar()).thenThrow(new RuntimeException("Auth error"));

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync/test"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Connection failed: Auth error"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getGoogleSyncSettings_Success() throws Exception {
        // Given
        GoogleSyncSettingsResponse settings = new GoogleSyncSettingsResponse(
                true, "primary", "BIDIRECTIONAL", true, 30
        );
        when(calendarSyncService.getGoogleSyncSettings()).thenReturn(settings);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.calendarId").value("primary"))
                .andExpect(jsonPath("$.syncDirection").value("BIDIRECTIONAL"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateGoogleSyncSettings_Success() throws Exception {
        // Given
        GoogleSyncSettingsRequest request = new GoogleSyncSettingsRequest(
                true, "primary", "BIDIRECTIONAL", true, 30
        );
        GoogleSyncSettingsResponse response = new GoogleSyncSettingsResponse(
                true, "primary", "BIDIRECTIONAL", true, 30
        );
        when(calendarSyncService.updateGoogleSyncSettings(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/calendar/sync/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void triggerManualSync_Success() throws Exception {
        // Given
        GoogleSyncStatusResponse status = new GoogleSyncStatusResponse(
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusHours(1).toString(),
                true,
                10,
                List.of()
        );
        when(calendarSyncService.triggerManualSync()).thenReturn(status);

        // When & Then
        mockMvc.perform(post("/api/v1/calendar/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").value(true))
                .andExpect(jsonPath("$.syncedEvents").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getGoogleSyncStatus_Success() throws Exception {
        // Given
        GoogleSyncStatusResponse status = new GoogleSyncStatusResponse(
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusHours(1).toString(),
                true,
                10,
                List.of()
        );
        when(calendarSyncService.getGoogleSyncStatus()).thenReturn(status);

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRunning").value(true))
                .andExpect(jsonPath("$.syncedEvents").value(10));
    }


    @Test
    @WithMockUser(roles = "USER")
    void getSyncSettings_Error_ReturnsBadRequest() throws Exception {
        // Given
        when(userContextService.getCurrentUserId()).thenThrow(new RuntimeException("Context error"));

        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings/list"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateGoogleSyncSettings_Error_ReturnsBadRequest() throws Exception {
        // Given
        GoogleSyncSettingsRequest request = new GoogleSyncSettingsRequest(
                true, "primary", "BIDIRECTIONAL", true, 30
        );
        when(calendarSyncService.updateGoogleSyncSettings(any())).thenThrow(new RuntimeException("Update failed"));

        // When & Then
        mockMvc.perform(put("/api/v1/calendar/sync/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}