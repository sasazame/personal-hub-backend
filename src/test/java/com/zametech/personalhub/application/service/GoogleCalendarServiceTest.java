package com.zametech.personalhub.application.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.zametech.personalhub.domain.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {
    
    @Mock
    private Calendar mockCalendar;
    
    @Mock
    private Calendar.CalendarList mockCalendarList;
    
    @Mock
    private Calendar.CalendarList.List mockCalendarListList;
    
    @Mock
    private Calendar.Events mockEvents;
    
    @Mock
    private Calendar.Events.List mockEventsList;
    
    @Mock
    private Calendar.Events.Insert mockEventsInsert;
    
    @Mock
    private Calendar.Events.Update mockEventsUpdate;
    
    @Mock
    private Calendar.Events.Delete mockEventsDelete;
    
    private GoogleCalendarService googleCalendarService;
    
    private String credentialsFilePath = "credentials.json";
    private String userCredentialsJson = "{\"type\":\"service_account\",\"project_id\":\"test\"}";
    
    @BeforeEach
    void setUp() {
        googleCalendarService = spy(new GoogleCalendarService());
        ReflectionTestUtils.setField(googleCalendarService, "credentialsFilePath", credentialsFilePath);
    }
    
    @Test
    void getCalendarService_withUserCredentials_shouldReturnCalendar() throws Exception {
        // Given
        try (MockedStatic<GoogleCredentials> credentialsMock = mockStatic(GoogleCredentials.class);
             MockedStatic<GoogleNetHttpTransport> transportMock = mockStatic(GoogleNetHttpTransport.class)) {
            
            GoogleCredentials mockCredentials = mock(GoogleCredentials.class);
            NetHttpTransport mockTransport = mock(NetHttpTransport.class);
            
            credentialsMock.when(() -> GoogleCredentials.fromStream(any(ByteArrayInputStream.class)))
                .thenReturn(mockCredentials);
            when(mockCredentials.createScoped(anyList())).thenReturn(mockCredentials);
            
            transportMock.when(GoogleNetHttpTransport::newTrustedTransport)
                .thenReturn(mockTransport);
            
            // When
            Calendar result = googleCalendarService.getCalendarService(userCredentialsJson);
            
            // Then
            assertThat(result).isNotNull();
        }
    }
    
    @Test
    void getCalendarService_withNoCredentials_shouldThrowException() {
        // Given
        ReflectionTestUtils.setField(googleCalendarService, "credentialsFilePath", "");
        
        // When & Then
        assertThatThrownBy(() -> googleCalendarService.getCalendarService(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Google Calendar credentials not configured. Please set GOOGLE_CREDENTIALS_FILE environment variable.");
    }
    
    @Test
    void getUserCalendars_withValidCredentials_shouldReturnCalendarList() throws Exception {
        // Given
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.calendarList()).thenReturn(mockCalendarList);
        when(mockCalendarList.list()).thenReturn(mockCalendarListList);
        
        CalendarList calendarListResult = new CalendarList();
        CalendarListEntry entry1 = new CalendarListEntry();
        entry1.setId("calendar1");
        entry1.setSummary("Calendar 1");
        CalendarListEntry entry2 = new CalendarListEntry();
        entry2.setId("calendar2");
        entry2.setSummary("Calendar 2");
        calendarListResult.setItems(Arrays.asList(entry1, entry2));
        
        when(mockCalendarListList.execute()).thenReturn(calendarListResult);
        
        // When
        List<CalendarListEntry> result = googleCalendarService.getUserCalendars(userCredentialsJson);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("calendar1");
        assertThat(result.get(1).getId()).isEqualTo("calendar2");
    }
    
    @Test
    void getUserCalendars_withEmptyList_shouldReturnEmptyList() throws Exception {
        // Given
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.calendarList()).thenReturn(mockCalendarList);
        when(mockCalendarList.list()).thenReturn(mockCalendarListList);
        
        CalendarList calendarListResult = new CalendarList();
        calendarListResult.setItems(null);
        
        when(mockCalendarListList.execute()).thenReturn(calendarListResult);
        
        // When
        List<CalendarListEntry> result = googleCalendarService.getUserCalendars(userCredentialsJson);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void getUserCalendars_withException_shouldReturnEmptyList() throws Exception {
        // Given
        doThrow(new IOException("Calendar API error"))
            .when(googleCalendarService).getCalendarService(userCredentialsJson);
        
        // When
        List<CalendarListEntry> result = googleCalendarService.getUserCalendars(userCredentialsJson);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void getCalendarEvents_withValidRange_shouldReturnEvents() throws Exception {
        // Given
        String calendarId = "primary";
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 31, 23, 59);
        
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.list(calendarId)).thenReturn(mockEventsList);
        when(mockEventsList.setTimeMin(any(DateTime.class))).thenReturn(mockEventsList);
        when(mockEventsList.setTimeMax(any(DateTime.class))).thenReturn(mockEventsList);
        when(mockEventsList.setOrderBy(anyString())).thenReturn(mockEventsList);
        when(mockEventsList.setSingleEvents(anyBoolean())).thenReturn(mockEventsList);
        
        Events eventsResult = new Events();
        com.google.api.services.calendar.model.Event event1 = new com.google.api.services.calendar.model.Event();
        event1.setId("event1");
        event1.setSummary("Event 1");
        com.google.api.services.calendar.model.Event event2 = new com.google.api.services.calendar.model.Event();
        event2.setId("event2");
        event2.setSummary("Event 2");
        eventsResult.setItems(Arrays.asList(event1, event2));
        
        when(mockEventsList.execute()).thenReturn(eventsResult);
        
        // When
        List<com.google.api.services.calendar.model.Event> result = 
            googleCalendarService.getCalendarEvents(userCredentialsJson, calendarId, startTime, endTime);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("event1");
        assertThat(result.get(1).getId()).isEqualTo("event2");
        
        // Verify date range
        ArgumentCaptor<DateTime> timeMinCaptor = ArgumentCaptor.forClass(DateTime.class);
        ArgumentCaptor<DateTime> timeMaxCaptor = ArgumentCaptor.forClass(DateTime.class);
        verify(mockEventsList).setTimeMin(timeMinCaptor.capture());
        verify(mockEventsList).setTimeMax(timeMaxCaptor.capture());
        
        long expectedStartMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long expectedEndMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertThat(timeMinCaptor.getValue().getValue()).isEqualTo(expectedStartMillis);
        assertThat(timeMaxCaptor.getValue().getValue()).isEqualTo(expectedEndMillis);
    }
    
    @Test
    void getCalendarEvents_withException_shouldReturnEmptyList() throws Exception {
        // Given
        String calendarId = "primary";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
        
        doThrow(new IOException("Calendar API error"))
            .when(googleCalendarService).getCalendarService(userCredentialsJson);
        
        // When
        List<com.google.api.services.calendar.model.Event> result = 
            googleCalendarService.getCalendarEvents(userCredentialsJson, calendarId, startTime, endTime);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void createCalendarEvent_withValidEvent_shouldReturnEventId() throws Exception {
        // Given
        String calendarId = "primary";
        Event personalHubEvent = new Event();
        personalHubEvent.setTitle("Test Event");
        personalHubEvent.setDescription("Test Description");
        personalHubEvent.setLocation("Test Location");
        personalHubEvent.setStartDateTime(LocalDateTime.of(2024, 1, 15, 10, 0));
        personalHubEvent.setEndDateTime(LocalDateTime.of(2024, 1, 15, 11, 0));
        personalHubEvent.setAllDay(false);
        personalHubEvent.setColor("#ff5722");
        
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq(calendarId), any(com.google.api.services.calendar.model.Event.class)))
            .thenReturn(mockEventsInsert);
        
        com.google.api.services.calendar.model.Event createdEvent = new com.google.api.services.calendar.model.Event();
        createdEvent.setId("created-event-id");
        when(mockEventsInsert.execute()).thenReturn(createdEvent);
        
        // When
        Optional<String> result = googleCalendarService.createCalendarEvent(
            userCredentialsJson, calendarId, personalHubEvent);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("created-event-id");
        
        // Verify event conversion
        ArgumentCaptor<com.google.api.services.calendar.model.Event> eventCaptor = 
            ArgumentCaptor.forClass(com.google.api.services.calendar.model.Event.class);
        verify(mockEvents).insert(eq(calendarId), eventCaptor.capture());
        
        com.google.api.services.calendar.model.Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSummary()).isEqualTo("Test Event");
        assertThat(capturedEvent.getDescription()).isEqualTo("Test Description");
        assertThat(capturedEvent.getLocation()).isEqualTo("Test Location");
        assertThat(capturedEvent.getColorId()).isEqualTo("11"); // Red color
    }
    
    @Test
    void createCalendarEvent_withAllDayEvent_shouldSetDateOnly() throws Exception {
        // Given
        String calendarId = "primary";
        Event personalHubEvent = new Event();
        personalHubEvent.setTitle("All Day Event");
        personalHubEvent.setStartDateTime(LocalDateTime.of(2024, 1, 15, 0, 0));
        personalHubEvent.setEndDateTime(LocalDateTime.of(2024, 1, 15, 23, 59));
        personalHubEvent.setAllDay(true);
        
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.insert(eq(calendarId), any(com.google.api.services.calendar.model.Event.class)))
            .thenReturn(mockEventsInsert);
        
        com.google.api.services.calendar.model.Event createdEvent = new com.google.api.services.calendar.model.Event();
        createdEvent.setId("all-day-event-id");
        when(mockEventsInsert.execute()).thenReturn(createdEvent);
        
        // When
        Optional<String> result = googleCalendarService.createCalendarEvent(
            userCredentialsJson, calendarId, personalHubEvent);
        
        // Then
        assertThat(result).isPresent();
        
        // Verify all-day event format
        ArgumentCaptor<com.google.api.services.calendar.model.Event> eventCaptor = 
            ArgumentCaptor.forClass(com.google.api.services.calendar.model.Event.class);
        verify(mockEvents).insert(eq(calendarId), eventCaptor.capture());
        
        com.google.api.services.calendar.model.Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getStart().getDate()).isNotNull();
        assertThat(capturedEvent.getStart().getDateTime()).isNull();
        assertThat(capturedEvent.getEnd().getDate()).isNotNull();
        assertThat(capturedEvent.getEnd().getDateTime()).isNull();
    }
    
    @Test
    void createCalendarEvent_withException_shouldReturnEmpty() throws Exception {
        // Given
        String calendarId = "primary";
        Event personalHubEvent = new Event();
        personalHubEvent.setTitle("Test Event");
        personalHubEvent.setStartDateTime(LocalDateTime.now());
        personalHubEvent.setEndDateTime(LocalDateTime.now().plusHours(1));
        
        doThrow(new IOException("Calendar API error"))
            .when(googleCalendarService).getCalendarService(userCredentialsJson);
        
        // When
        Optional<String> result = googleCalendarService.createCalendarEvent(
            userCredentialsJson, calendarId, personalHubEvent);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void updateCalendarEvent_withValidEvent_shouldReturnTrue() throws Exception {
        // Given
        String calendarId = "primary";
        String eventId = "event-to-update";
        Event personalHubEvent = new Event();
        personalHubEvent.setTitle("Updated Event");
        personalHubEvent.setStartDateTime(LocalDateTime.now());
        personalHubEvent.setEndDateTime(LocalDateTime.now().plusHours(1));
        
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.update(eq(calendarId), eq(eventId), any(com.google.api.services.calendar.model.Event.class)))
            .thenReturn(mockEventsUpdate);
        when(mockEventsUpdate.execute()).thenReturn(new com.google.api.services.calendar.model.Event());
        
        // When
        boolean result = googleCalendarService.updateCalendarEvent(
            userCredentialsJson, calendarId, eventId, personalHubEvent);
        
        // Then
        assertThat(result).isTrue();
        verify(mockEventsUpdate).execute();
    }
    
    @Test
    void updateCalendarEvent_withException_shouldReturnFalse() throws Exception {
        // Given
        String calendarId = "primary";
        String eventId = "event-to-update";
        Event personalHubEvent = new Event();
        personalHubEvent.setTitle("Updated Event");
        personalHubEvent.setStartDateTime(LocalDateTime.now());
        personalHubEvent.setEndDateTime(LocalDateTime.now().plusHours(1));
        
        doThrow(new IOException("Calendar API error"))
            .when(googleCalendarService).getCalendarService(userCredentialsJson);
        
        // When
        boolean result = googleCalendarService.updateCalendarEvent(
            userCredentialsJson, calendarId, eventId, personalHubEvent);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void deleteCalendarEvent_withValidEventId_shouldReturnTrue() throws Exception {
        // Given
        String calendarId = "primary";
        String eventId = "event-to-delete";
        
        doReturn(mockCalendar).when(googleCalendarService).getCalendarService(userCredentialsJson);
        when(mockCalendar.events()).thenReturn(mockEvents);
        when(mockEvents.delete(calendarId, eventId)).thenReturn(mockEventsDelete);
        doNothing().when(mockEventsDelete).execute();
        
        // When
        boolean result = googleCalendarService.deleteCalendarEvent(userCredentialsJson, calendarId, eventId);
        
        // Then
        assertThat(result).isTrue();
        verify(mockEventsDelete).execute();
    }
    
    @Test
    void deleteCalendarEvent_withException_shouldReturnFalse() throws Exception {
        // Given
        String calendarId = "primary";
        String eventId = "event-to-delete";
        
        doThrow(new IOException("Calendar API error"))
            .when(googleCalendarService).getCalendarService(userCredentialsJson);
        
        // When
        boolean result = googleCalendarService.deleteCalendarEvent(userCredentialsJson, calendarId, eventId);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void convertColorToGoogleColorId_shouldMapCorrectly() {
        // Use reflection to test private method
        assertThat((String) ReflectionTestUtils.invokeMethod(googleCalendarService, "convertColorToGoogleColorId", "#ff5722"))
            .isEqualTo("11"); // Red
        assertThat((String) ReflectionTestUtils.invokeMethod(googleCalendarService, "convertColorToGoogleColorId", "#ff9800"))
            .isEqualTo("6"); // Orange
        assertThat((String) ReflectionTestUtils.invokeMethod(googleCalendarService, "convertColorToGoogleColorId", "#2196f3"))
            .isEqualTo("9"); // Blue
        assertThat((String) ReflectionTestUtils.invokeMethod(googleCalendarService, "convertColorToGoogleColorId", "unknown"))
            .isEqualTo("1"); // Default
    }
    
    @Test
    void convertFromGoogleEvent_withTimedEvent_shouldConvertCorrectly() {
        // Given
        UUID userId = UUID.randomUUID();
        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event();
        googleEvent.setId("google-event-id");
        googleEvent.setSummary("Google Event");
        googleEvent.setDescription("Google Description");
        googleEvent.setLocation("Google Location");
        
        long startMillis = System.currentTimeMillis();
        long endMillis = startMillis + 3600000; // 1 hour later
        
        EventDateTime start = new EventDateTime();
        start.setDateTime(new DateTime(startMillis));
        googleEvent.setStart(start);
        
        EventDateTime end = new EventDateTime();
        end.setDateTime(new DateTime(endMillis));
        googleEvent.setEnd(end);
        
        // When
        Event result = googleCalendarService.convertFromGoogleEvent(googleEvent, userId);
        
        // Then
        assertThat(result.getTitle()).isEqualTo("Google Event");
        assertThat(result.getDescription()).isEqualTo("Google Description");
        assertThat(result.getLocation()).isEqualTo("Google Location");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isAllDay()).isFalse();
        assertThat(result.getGoogleEventId()).isEqualTo("google-event-id");
        assertThat(result.getSyncStatus()).isEqualTo("SYNCED");
        assertThat(result.getLastSyncedAt()).isNotNull();
    }
    
    @Test
    void convertFromGoogleEvent_withAllDayEvent_shouldConvertCorrectly() {
        // Given
        UUID userId = UUID.randomUUID();
        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event();
        googleEvent.setId("all-day-event");
        googleEvent.setSummary("All Day Event");
        
        EventDateTime start = new EventDateTime();
        start.setDate(new DateTime("2024-01-15"));
        googleEvent.setStart(start);
        
        EventDateTime end = new EventDateTime();
        end.setDate(new DateTime("2024-01-16"));
        googleEvent.setEnd(end);
        
        // When
        Event result = googleCalendarService.convertFromGoogleEvent(googleEvent, userId);
        
        // Then
        assertThat(result.isAllDay()).isTrue();
        assertThat(result.getStartDateTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 0, 0, 0));
        assertThat(result.getEndDateTime()).isEqualTo(LocalDateTime.of(2024, 1, 16, 23, 59, 59));
    }
}