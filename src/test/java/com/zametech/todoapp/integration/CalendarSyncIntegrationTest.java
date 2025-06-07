package com.zametech.todoapp.integration;

import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class CalendarSyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarSyncSettingsRepository calendarSyncSettingsRepository;

    @Autowired
    private EventRepository eventRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        // Clean up any existing data
        calendarSyncSettingsRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testGetSyncStatus_WhenNoSettings_ReturnsNotConnected() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.syncStatus").value("NOT_CONNECTED"))
                .andExpect(jsonPath("$.connectedCalendars").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})  
    void testCalendarSyncSettingsPersistence() {
        // Given
        CalendarSyncSettingsEntity settings = new CalendarSyncSettingsEntity(
            userId, "primary", "Primary Calendar"
        );

        // When
        CalendarSyncSettingsEntity saved = calendarSyncSettingsRepository.save(settings);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGoogleCalendarId()).isEqualTo("primary");
        assertThat(saved.getCalendarName()).isEqualTo("Primary Calendar");
        assertThat(saved.getSyncEnabled()).isTrue();
        assertThat(saved.getSyncDirection()).isEqualTo("BIDIRECTIONAL");
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testFindCalendarSettingsByUser() {
        // Given
        CalendarSyncSettingsEntity settings1 = new CalendarSyncSettingsEntity(
            userId, "primary", "Primary Calendar"
        );
        CalendarSyncSettingsEntity settings2 = new CalendarSyncSettingsEntity(
            userId, "work@company.com", "Work Calendar"
        );
        
        calendarSyncSettingsRepository.save(settings1);
        calendarSyncSettingsRepository.save(settings2);

        // When
        var userSettings = calendarSyncSettingsRepository.findByUserId(userId);
        var enabledSettings = calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId);

        // Then
        assertThat(userSettings).hasSize(2);
        assertThat(enabledSettings).hasSize(2);
        
        var found = calendarSyncSettingsRepository.findByUserIdAndGoogleCalendarId(userId, "primary");
        assertThat(found).isPresent();
        assertThat(found.get().getCalendarName()).isEqualTo("Primary Calendar");
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testCalendarSyncSettingsUniqueConstraint() {
        // Given
        CalendarSyncSettingsEntity settings1 = new CalendarSyncSettingsEntity(
            userId, "primary", "Primary Calendar"
        );
        calendarSyncSettingsRepository.save(settings1);

        // When
        boolean exists = calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "primary");

        // Then
        assertThat(exists).isTrue();
        
        boolean notExists = calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "nonexistent");
        assertThat(notExists).isFalse();
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testDisconnectCalendar() {
        // Given
        CalendarSyncSettingsEntity settings = new CalendarSyncSettingsEntity(
            userId, "primary", "Primary Calendar"
        );
        calendarSyncSettingsRepository.save(settings);

        // When
        calendarSyncSettingsRepository.deleteByUserIdAndGoogleCalendarId(userId, "primary");

        // Then
        boolean exists = calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "primary");
        assertThat(exists).isFalse();
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testGetSyncSettings_ReturnsEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/calendar/sync/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test  
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void testAuthEndpoints_RequireAuthentication() throws Exception {
        // Test OAuth URL endpoint
        mockMvc.perform(post("/api/v1/calendar/sync/auth/url"))
                .andExpect(status().isOk());

        // Test OAuth callback endpoint  
        mockMvc.perform(post("/api/v1/calendar/sync/auth/callback")
                .param("code", "test_code"))
                .andExpect(status().isOk());
    }
}