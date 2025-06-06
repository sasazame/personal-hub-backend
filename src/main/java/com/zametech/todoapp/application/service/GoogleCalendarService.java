package com.zametech.todoapp.application.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Personal Hub Calendar Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    @Value("${google.calendar.credentials.file:}")
    private String credentialsFilePath;
    
    @Value("${google.calendar.tokens.directory.path:tokens}")
    private String tokensDirectoryPath;

    /**
     * Create Calendar service with authorized credentials
     */
    public Calendar getCalendarService(String userCredentialsJson) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        // Build flow and trigger user authorization request
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
            new InputStreamReader(new ByteArrayInputStream(userCredentialsJson.getBytes())));
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singletonList(CalendarScopes.CALENDAR))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Get list of user's calendars
     */
    public List<CalendarListEntry> getUserCalendars(String userCredentialsJson) {
        try {
            Calendar service = getCalendarService(userCredentialsJson);
            CalendarList calendarList = service.calendarList().list().execute();
            return calendarList.getItems();
        } catch (Exception e) {
            log.error("Error fetching user calendars: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get events from Google Calendar
     */
    public List<Event> getCalendarEvents(String userCredentialsJson, String calendarId, 
                                       LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Calendar service = getCalendarService(userCredentialsJson);
            
            DateTime timeMin = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            DateTime timeMax = new DateTime(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            
            Events events = service.events().list(calendarId)
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            
            return events.getItems();
        } catch (Exception e) {
            log.error("Error fetching calendar events: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Create event in Google Calendar
     */
    public Optional<String> createCalendarEvent(String userCredentialsJson, String calendarId, 
                                              com.zametech.todoapp.domain.model.Event personalHubEvent) {
        try {
            Calendar service = getCalendarService(userCredentialsJson);
            Event googleEvent = convertToGoogleEvent(personalHubEvent);
            
            Event createdEvent = service.events().insert(calendarId, googleEvent).execute();
            log.info("Created Google Calendar event: {}", createdEvent.getId());
            
            return Optional.of(createdEvent.getId());
        } catch (Exception e) {
            log.error("Error creating calendar event: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Update event in Google Calendar
     */
    public boolean updateCalendarEvent(String userCredentialsJson, String calendarId, String eventId,
                                     com.zametech.todoapp.domain.model.Event personalHubEvent) {
        try {
            Calendar service = getCalendarService(userCredentialsJson);
            Event googleEvent = convertToGoogleEvent(personalHubEvent);
            googleEvent.setId(eventId);
            
            service.events().update(calendarId, eventId, googleEvent).execute();
            log.info("Updated Google Calendar event: {}", eventId);
            
            return true;
        } catch (Exception e) {
            log.error("Error updating calendar event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete event from Google Calendar
     */
    public boolean deleteCalendarEvent(String userCredentialsJson, String calendarId, String eventId) {
        try {
            Calendar service = getCalendarService(userCredentialsJson);
            service.events().delete(calendarId, eventId).execute();
            log.info("Deleted Google Calendar event: {}", eventId);
            
            return true;
        } catch (Exception e) {
            log.error("Error deleting calendar event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert Personal Hub Event to Google Calendar Event
     */
    private Event convertToGoogleEvent(com.zametech.todoapp.domain.model.Event personalHubEvent) {
        Event googleEvent = new Event()
                .setSummary(personalHubEvent.getTitle())
                .setDescription(personalHubEvent.getDescription())
                .setLocation(personalHubEvent.getLocation());

        ZoneId systemZone = ZoneId.systemDefault();
        
        if (personalHubEvent.isAllDay()) {
            // All day event
            EventDateTime start = new EventDateTime()
                    .setDate(new DateTime(personalHubEvent.getStartDateTime().toLocalDate().toString()));
            googleEvent.setStart(start);
            
            EventDateTime end = new EventDateTime()
                    .setDate(new DateTime(personalHubEvent.getEndDateTime().toLocalDate().toString()));
            googleEvent.setEnd(end);
        } else {
            // Timed event
            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(personalHubEvent.getStartDateTime().atZone(systemZone).toInstant().toEpochMilli()))
                    .setTimeZone(systemZone.getId());
            googleEvent.setStart(start);
            
            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(personalHubEvent.getEndDateTime().atZone(systemZone).toInstant().toEpochMilli()))
                    .setTimeZone(systemZone.getId());
            googleEvent.setEnd(end);
        }

        // Set color if available
        if (personalHubEvent.getColor() != null) {
            // Google Calendar uses numbered color IDs, might need conversion
            googleEvent.setColorId(convertColorToGoogleColorId(personalHubEvent.getColor()));
        }

        return googleEvent;
    }

    /**
     * Convert Personal Hub color to Google Calendar color ID
     */
    private String convertColorToGoogleColorId(String color) {
        // Google Calendar uses numbered color IDs (1-11)
        // This is a simple mapping, can be enhanced based on requirements
        return switch (color.toLowerCase()) {
            case "#ff5722", "red" -> "11";
            case "#ff9800", "orange" -> "6";
            case "#ffc107", "yellow" -> "5";
            case "#4caf50", "green" -> "10";
            case "#2196f3", "blue" -> "9";
            case "#9c27b0", "purple" -> "3";
            default -> "1"; // Default blue
        };
    }

    /**
     * Convert Google Calendar Event to Personal Hub Event
     */
    public com.zametech.todoapp.domain.model.Event convertFromGoogleEvent(Event googleEvent, Long userId) {
        com.zametech.todoapp.domain.model.Event personalHubEvent = new com.zametech.todoapp.domain.model.Event();
        
        personalHubEvent.setTitle(googleEvent.getSummary());
        personalHubEvent.setDescription(googleEvent.getDescription());
        personalHubEvent.setLocation(googleEvent.getLocation());
        personalHubEvent.setUserId(userId);

        // Convert times
        EventDateTime googleStart = googleEvent.getStart();
        EventDateTime googleEnd = googleEvent.getEnd();
        
        if (googleStart.getDate() != null) {
            // All day event
            personalHubEvent.setAllDay(true);
            personalHubEvent.setStartDateTime(LocalDateTime.of(
                new DateTime(googleStart.getDate().getValue()).toStringRfc3339().substring(0, 10) + "T00:00:00", 
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            personalHubEvent.setEndDateTime(LocalDateTime.of(
                new DateTime(googleEnd.getDate().getValue()).toStringRfc3339().substring(0, 10) + "T23:59:59", 
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            // Timed event
            personalHubEvent.setAllDay(false);
            personalHubEvent.setStartDateTime(
                ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleStart.getDateTime().getValue()),
                    ZoneId.systemDefault()
                ).toLocalDateTime()
            );
            personalHubEvent.setEndDateTime(
                ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleEnd.getDateTime().getValue()),
                    ZoneId.systemDefault()
                ).toLocalDateTime()
            );
        }

        // Set sync fields
        personalHubEvent.setGoogleEventId(googleEvent.getId());
        personalHubEvent.setSyncStatus("SYNCED");
        personalHubEvent.setLastSyncedAt(LocalDateTime.now());

        return personalHubEvent;
    }
}