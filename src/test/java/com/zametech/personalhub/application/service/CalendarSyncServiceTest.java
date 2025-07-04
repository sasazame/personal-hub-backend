package com.zametech.personalhub.application.service;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.zametech.personalhub.common.exception.OAuth2RequiredException;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.model.UserSocialAccount;
import com.zametech.personalhub.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.personalhub.domain.repository.EventRepository;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.domain.repository.UserSocialAccountRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.personalhub.presentation.dto.request.GoogleSyncSettingsRequest;
import com.zametech.personalhub.presentation.dto.response.CalendarSyncStatusResponse;
import com.zametech.personalhub.presentation.dto.response.CalendarSyncSettingsResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncSettingsResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository socialAccountRepository;

    @Mock
    private GoogleCalendarOAuth2Service googleCalendarOAuth2Service;

    @InjectMocks
    private CalendarSyncService calendarSyncService;

    private UUID userId;
    private User testUser;
    private CalendarSyncSettingsEntity syncSettings;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        
        syncSettings = new CalendarSyncSettingsEntity(
            userId,
            "primary",
            "Primary Calendar"
        );
        syncSettings.setId(1L);
        syncSettings.setLastSyncAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void getSyncStatus_WhenNoSettings_ReturnsNotConnected() {
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
    void getSyncStatus_WhenSettingsExist_ReturnsConnected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of(syncSettings));
        when(eventRepository.findByUserIdAndDateRange(
            eq(userId),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
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
    void getSyncStatus_CalculatesStatisticsCorrectly() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of(syncSettings));
        
        // Mock events with different sync statuses
        List<com.zametech.personalhub.domain.model.Event> events = List.of(
            createMockEvent("SYNCED"),
            createMockEvent("SYNC_PENDING"),
            createMockEvent("SYNC_ERROR"),
            createMockEvent("NONE")
        );
        
        when(eventRepository.findByUserIdAndDateRange(
            eq(userId),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
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

    @Test
    void connectGoogleCalendar_WhenNoSocialAccount_ThrowsOAuth2RequiredException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> calendarSyncService.connectGoogleCalendar())
            .isInstanceOf(OAuth2RequiredException.class)
            .hasMessageContaining("Please login with Google OAuth2 first");
    }

    @Test
    void connectGoogleCalendar_Success() throws Exception {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        UserSocialAccount socialAccount = UserSocialAccount.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .provider("google")
            .email("google@example.com")
            .build();
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google")).thenReturn(Optional.of(socialAccount));
        
        CalendarListEntry calendar1 = new CalendarListEntry();
        calendar1.setId("primary");
        calendar1.setSummary("Primary Calendar");
        
        CalendarListEntry calendar2 = new CalendarListEntry();
        calendar2.setId("work");
        calendar2.setSummary("Work Calendar");
        
        List<CalendarListEntry> calendars = List.of(calendar1, calendar2);
        when(googleCalendarOAuth2Service.getUserCalendars(testUser)).thenReturn(calendars);
        
        when(calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "primary")).thenReturn(false);
        when(calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "work")).thenReturn(false);

        // When
        List<CalendarListEntry> result = calendarSyncService.connectGoogleCalendar();

        // Then
        assertThat(result).hasSize(2);
        verify(calendarSyncSettingsRepository, times(2)).save(any(CalendarSyncSettingsEntity.class));
    }

    @Test
    void connectGoogleCalendar_WhenCalendarAlreadyExists_SkipsSaving() throws Exception {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        UserSocialAccount socialAccount = UserSocialAccount.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .provider("google")
            .email("google@example.com")
            .build();
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google")).thenReturn(Optional.of(socialAccount));
        
        CalendarListEntry calendar = new CalendarListEntry();
        calendar.setId("primary");
        calendar.setSummary("Primary Calendar");
        
        when(googleCalendarOAuth2Service.getUserCalendars(testUser)).thenReturn(List.of(calendar));
        when(calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, "primary")).thenReturn(true);

        // When
        List<CalendarListEntry> result = calendarSyncService.connectGoogleCalendar();

        // Then
        assertThat(result).hasSize(1);
        verify(calendarSyncSettingsRepository, never()).save(any(CalendarSyncSettingsEntity.class));
    }

    @Test
    void disconnectGoogleCalendar_Success() {
        // Given
        String calendarId = "primary";
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        
        com.zametech.personalhub.domain.model.Event syncedEvent = createMockEvent("SYNCED");
        syncedEvent.setGoogleCalendarId(calendarId);
        syncedEvent.setGoogleEventId("google-event-123");
        
        when(eventRepository.findByUserIdAndSyncStatus(userId, "SYNCED"))
            .thenReturn(List.of(syncedEvent));

        // When
        calendarSyncService.disconnectGoogleCalendar(calendarId);

        // Then
        verify(calendarSyncSettingsRepository).deleteByUserIdAndGoogleCalendarId(userId, calendarId);
        verify(eventRepository).save(argThat(event -> 
            event.getGoogleCalendarId() == null &&
            event.getGoogleEventId() == null &&
            "NONE".equals(event.getSyncStatus()) &&
            event.getLastSyncedAt() == null
        ));
    }

    @Test
    void performSync_WhenNoSettings_ReturnsNotConnected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId)).thenReturn(List.of());

        // When
        CalendarSyncStatusResponse response = calendarSyncService.performSync();

        // Then
        assertThat(response.isConnected()).isFalse();
        assertThat(response.syncStatus()).isEqualTo("SUCCESS");
        assertThat(response.connectedCalendars()).isEmpty();
    }

    @Test
    void performSync_WithBidirectionalSync_Success() throws Exception {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        syncSettings.setSyncEnabled(true);
        syncSettings.setSyncDirection("BIDIRECTIONAL");
        when(calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId)).thenReturn(List.of(syncSettings));
        
        // Mock Google events
        Event googleEvent = new Event();
        googleEvent.setId("google-event-1");
        googleEvent.setSummary("Google Event");
        
        when(googleCalendarOAuth2Service.getCalendarEvents(
            eq(testUser), 
            eq("primary"), 
            any(LocalDateTime.class), 
            any(LocalDateTime.class)
        )).thenReturn(List.of(googleEvent));
        
        when(eventRepository.findByGoogleEventId("google-event-1")).thenReturn(Optional.empty());
        
        // Mock local events
        com.zametech.personalhub.domain.model.Event localEvent = createMockEvent("SYNC_PENDING");
        when(eventRepository.findByUserIdAndSyncStatus(userId, "SYNC_PENDING"))
            .thenReturn(List.of(localEvent));
        
        // Mock creating event in Google Calendar (for bidirectional sync)
        when(googleCalendarOAuth2Service.createCalendarEvent(eq(testUser), eq("primary"), any()))
            .thenReturn(Optional.of("new-google-event-id"));

        // When
        CalendarSyncStatusResponse response = calendarSyncService.performSync();

        // Then
        assertThat(response.isConnected()).isTrue();
        assertThat(response.syncStatus()).isEqualTo("SUCCESS");
        verify(calendarSyncSettingsRepository).save(any(CalendarSyncSettingsEntity.class));
    }

    @Test
    void updateGoogleSyncSettings_Success() {
        // Given
        GoogleSyncSettingsRequest request = new GoogleSyncSettingsRequest(
            true,
            "primary",
            "BIDIRECTIONAL",
            true,
            30
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserIdAndGoogleCalendarId(userId, "primary"))
            .thenReturn(Optional.of(syncSettings));
        
        CalendarSyncSettingsEntity savedSettings = new CalendarSyncSettingsEntity(userId, "primary", "Primary Calendar");
        savedSettings.setSyncEnabled(true);
        savedSettings.setSyncDirection("BIDIRECTIONAL");
        savedSettings.setAutoSync(true);
        savedSettings.setSyncInterval(30);
        
        when(calendarSyncSettingsRepository.save(any(CalendarSyncSettingsEntity.class)))
            .thenReturn(savedSettings);

        // When
        GoogleSyncSettingsResponse response = calendarSyncService.updateGoogleSyncSettings(request);

        // Then
        assertThat(response.calendarId()).isEqualTo("primary");
        assertThat(response.enabled()).isTrue();
        assertThat(response.syncDirection()).isEqualTo("BIDIRECTIONAL");
        verify(calendarSyncSettingsRepository).save(any(CalendarSyncSettingsEntity.class));
    }

    @Test
    void updateGoogleSyncSettings_WhenNotFound_CreatesNew() {
        // Given
        GoogleSyncSettingsRequest request = new GoogleSyncSettingsRequest(
            true,
            "nonexistent",
            "BIDIRECTIONAL",
            true,
            30
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserIdAndGoogleCalendarId(userId, "nonexistent"))
            .thenReturn(Optional.empty());
        
        CalendarSyncSettingsEntity savedSettings = new CalendarSyncSettingsEntity(userId, "nonexistent", "Primary Calendar");
        savedSettings.setSyncEnabled(true);
        savedSettings.setSyncDirection("BIDIRECTIONAL");
        savedSettings.setAutoSync(true);
        savedSettings.setSyncInterval(30);
        
        when(calendarSyncSettingsRepository.save(any(CalendarSyncSettingsEntity.class)))
            .thenReturn(savedSettings);

        // When
        GoogleSyncSettingsResponse response = calendarSyncService.updateGoogleSyncSettings(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.calendarId()).isEqualTo("nonexistent");
        assertThat(response.enabled()).isTrue();
        verify(calendarSyncSettingsRepository).save(any(CalendarSyncSettingsEntity.class));
    }

    @Test
    void getGoogleSyncSettings_Connected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        
        syncSettings.setSyncEnabled(true);
        syncSettings.setSyncDirection("BIDIRECTIONAL");
        syncSettings.setAutoSync(true);
        syncSettings.setSyncInterval(30);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of(syncSettings));

        // When
        GoogleSyncSettingsResponse response = calendarSyncService.getGoogleSyncSettings();

        // Then
        assertThat(response.enabled()).isTrue();
        assertThat(response.calendarId()).isEqualTo("primary");
        assertThat(response.syncDirection()).isEqualTo("BIDIRECTIONAL");
        assertThat(response.autoSync()).isTrue();
        assertThat(response.syncInterval()).isEqualTo(30);
    }

    @Test
    void getGoogleSyncSettings_NotConnected() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(calendarSyncSettingsRepository.findByUserId(userId)).thenReturn(List.of());

        // When
        GoogleSyncSettingsResponse response = calendarSyncService.getGoogleSyncSettings();

        // Then
        assertThat(response.enabled()).isFalse();
        assertThat(response.calendarId()).isNull();
        assertThat(response.syncDirection()).isEqualTo("BIDIRECTIONAL");
        assertThat(response.autoSync()).isFalse();
        assertThat(response.syncInterval()).isEqualTo(30);
    }

    private com.zametech.personalhub.domain.model.Event createMockEvent(String syncStatus) {
        com.zametech.personalhub.domain.model.Event event = new com.zametech.personalhub.domain.model.Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setUserId(userId);
        event.setSyncStatus(syncStatus);
        event.setStartDateTime(LocalDateTime.now());
        event.setEndDateTime(LocalDateTime.now().plusHours(1));
        return event;
    }
}