package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CalendarSyncSettingsRepository calendarSyncSettingsRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private CalendarSyncService calendarSyncService;

    private UUID userId;
    private CalendarSyncSettingsEntity syncSettings;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        syncSettings = new CalendarSyncSettingsEntity(
            userId,
            "primary",
            "Primary Calendar"
        );
        syncSettings.setId(1L);
        syncSettings.setLastSyncAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void testGetSyncStatus_WhenNoSettings_ReturnsNotConnected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of());

        // When
        CalendarSyncStatusResponse response = calendarSyncService.getSyncStatus();

        // Then
        assertThat(response.isConnected()).isFalse();
        assertThat(response.syncStatus()).isEqualTo("NOT_CONNECTED");
        assertThat(response.connectedCalendars()).isEmpty();
    }

    @Test
    void testGetSyncStatus_WhenSettingsExist_ReturnsConnected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of(syncSettings));
        when(eventRepository.findByUserIdAndDateRange(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(List.of());

        // When
        CalendarSyncStatusResponse response = calendarSyncService.getSyncStatus();

        // Then
        assertThat(response.isConnected()).isTrue();
        assertThat(response.connectedCalendars()).hasSize(1);
        assertThat(response.connectedCalendars().get(0).googleCalendarId()).isEqualTo("primary");
        assertThat(response.connectedCalendars().get(0).calendarName()).isEqualTo("Primary Calendar");
    }

    @Test
    void testGetSyncStatus_CalculatesStatisticsCorrectly() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of(syncSettings));
        
        // Mock events with different sync statuses
        List<com.zametech.todoapp.domain.model.Event> events = List.of(
            createMockEvent("SYNCED"),
            createMockEvent("SYNC_PENDING"),
            createMockEvent("SYNC_ERROR"),
            createMockEvent("NONE")
        );
        
        when(eventRepository.findByUserIdAndDateRange(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        )).thenReturn(events);

        // When
        CalendarSyncStatusResponse response = calendarSyncService.getSyncStatus();

        // Then
        CalendarSyncStatusResponse.SyncStatistics stats = response.syncStatistics();
        assertThat(stats.totalEvents()).isEqualTo(4);
        assertThat(stats.syncedEvents()).isEqualTo(1);
        assertThat(stats.pendingEvents()).isEqualTo(1);
        assertThat(stats.errorEvents()).isEqualTo(1);
    }

    private com.zametech.todoapp.domain.model.Event createMockEvent(String syncStatus) {
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setUserId(userId);
        event.setSyncStatus(syncStatus);
        event.setStartDateTime(LocalDateTime.now());
        event.setEndDateTime(LocalDateTime.now().plusHours(1));
        return event;
    }
}