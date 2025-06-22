package com.zametech.todoapp.application.service;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncService {

    private final GoogleCalendarService googleCalendarService;
    private final EventRepository eventRepository;
    private final CalendarSyncSettingsRepository calendarSyncSettingsRepository;
    private final UserContextService userContextService;

    /**
     * Connect user to Google Calendar
     */
    @Transactional
    public List<CalendarListEntry> connectGoogleCalendar(String userCredentialsJson) {
        Long userId = userContextService.getCurrentUserIdAsLong();
        
        try {
            List<CalendarListEntry> calendars = googleCalendarService.getUserCalendars(userCredentialsJson);
            
            // Save calendar settings for each calendar
            for (CalendarListEntry calendar : calendars) {
                if (!calendarSyncSettingsRepository.existsByUserIdAndGoogleCalendarId(userId, calendar.getId())) {
                    CalendarSyncSettingsEntity settings = new CalendarSyncSettingsEntity(
                        userId, 
                        calendar.getId(), 
                        calendar.getSummary()
                    );
                    calendarSyncSettingsRepository.save(settings);
                    log.info("Connected calendar {} for user {}", calendar.getSummary(), userId);
                }
            }
            
            return calendars;
        } catch (Exception e) {
            log.error("Error connecting Google Calendar for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to connect Google Calendar", e);
        }
    }

    /**
     * Disconnect user from Google Calendar
     */
    @Transactional
    public void disconnectGoogleCalendar(String calendarId) {
        Long userId = userContextService.getCurrentUserIdAsLong();
        
        try {
            calendarSyncSettingsRepository.deleteByUserIdAndGoogleCalendarId(userId, calendarId);
            
            // Update events to remove sync information
            List<com.zametech.todoapp.domain.model.Event> syncedEvents = 
                eventRepository.findByUserIdAndSyncStatus(userId, "SYNCED");
            
            for (com.zametech.todoapp.domain.model.Event event : syncedEvents) {
                if (calendarId.equals(event.getGoogleCalendarId())) {
                    event.setGoogleCalendarId(null);
                    event.setGoogleEventId(null);
                    event.setSyncStatus("NONE");
                    event.setLastSyncedAt(null);
                    eventRepository.save(event);
                }
            }
            
            log.info("Disconnected calendar {} for user {}", calendarId, userId);
        } catch (Exception e) {
            log.error("Error disconnecting Google Calendar for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to disconnect Google Calendar", e);
        }
    }

    /**
     * Perform bidirectional sync for all enabled calendars
     */
    @Transactional
    public CalendarSyncStatusResponse performSync(String userCredentialsJson) {
        Long userId = userContextService.getCurrentUserIdAsLong();
        
        try {
            List<CalendarSyncSettingsEntity> settings = 
                calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId);
            
            int totalEvents = 0;
            int syncedEvents = 0;
            int pendingEvents = 0;
            int errorEvents = 0;
            LocalDateTime lastSuccessfulSync = null;
            
            for (CalendarSyncSettingsEntity setting : settings) {
                SyncResult result = syncCalendar(userCredentialsJson, setting);
                totalEvents += result.totalEvents();
                syncedEvents += result.syncedEvents();
                pendingEvents += result.pendingEvents();
                errorEvents += result.errorEvents();
                
                if (result.lastSuccessfulSync() != null && 
                    (lastSuccessfulSync == null || result.lastSuccessfulSync().isAfter(lastSuccessfulSync))) {
                    lastSuccessfulSync = result.lastSuccessfulSync();
                }
                
                // Update last sync time
                setting.setLastSyncAt(LocalDateTime.now());
                calendarSyncSettingsRepository.save(setting);
            }
            
            // Build response
            CalendarSyncStatusResponse.SyncStatistics stats = 
                new CalendarSyncStatusResponse.SyncStatistics(
                    totalEvents, syncedEvents, pendingEvents, errorEvents, lastSuccessfulSync
                );
            
            return new CalendarSyncStatusResponse(
                !settings.isEmpty(),
                lastSuccessfulSync,
                errorEvents > 0 ? "ERROR" : "SUCCESS",
                settings.stream().map(this::toResponse).toList(),
                stats
            );
            
        } catch (Exception e) {
            log.error("Error performing sync for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to perform calendar sync", e);
        }
    }

    /**
     * Sync a single calendar
     */
    private SyncResult syncCalendar(String userCredentialsJson, CalendarSyncSettingsEntity setting) {
        try {
            String calendarId = setting.getGoogleCalendarId();
            String syncDirection = setting.getSyncDirection();
            LocalDateTime lastSyncAt = setting.getLastSyncAt() != null ? 
                setting.getLastSyncAt() : LocalDateTime.now().minusDays(30);
            
            int totalEvents = 0;
            int syncedEvents = 0;
            int pendingEvents = 0;
            int errorEvents = 0;
            
            // Sync from Google Calendar to Personal Hub
            if ("BIDIRECTIONAL".equals(syncDirection) || "FROM_GOOGLE".equals(syncDirection)) {
                SyncResult fromGoogleResult = syncFromGoogle(userCredentialsJson, calendarId, lastSyncAt);
                totalEvents += fromGoogleResult.totalEvents();
                syncedEvents += fromGoogleResult.syncedEvents();
                errorEvents += fromGoogleResult.errorEvents();
            }
            
            // Sync from Personal Hub to Google Calendar
            if ("BIDIRECTIONAL".equals(syncDirection) || "TO_GOOGLE".equals(syncDirection)) {
                SyncResult toGoogleResult = syncToGoogle(userCredentialsJson, calendarId, setting.getUserId());
                totalEvents += toGoogleResult.totalEvents();
                syncedEvents += toGoogleResult.syncedEvents();
                errorEvents += toGoogleResult.errorEvents();
            }
            
            return new SyncResult(
                totalEvents, syncedEvents, pendingEvents, errorEvents, LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error syncing calendar {}: {}", setting.getGoogleCalendarId(), e.getMessage(), e);
            return new SyncResult(0, 0, 0, 1, null);
        }
    }

    /**
     * Sync events from Google Calendar to Personal Hub
     */
    private SyncResult syncFromGoogle(String userCredentialsJson, String calendarId, LocalDateTime lastSyncAt) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> googleEvents = googleCalendarService.getCalendarEvents(
                userCredentialsJson, calendarId, lastSyncAt, now.plusDays(365)
            );
            
            int totalEvents = googleEvents.size();
            int syncedEvents = 0;
            int errorEvents = 0;
            
            Long userId = userContextService.getCurrentUserIdAsLong();
            
            for (Event googleEvent : googleEvents) {
                try {
                    // Check if event already exists
                    Optional<com.zametech.todoapp.domain.model.Event> existingEvent = 
                        eventRepository.findByGoogleEventId(googleEvent.getId());
                    
                    if (existingEvent.isPresent()) {
                        // Update existing event
                        com.zametech.todoapp.domain.model.Event personalHubEvent = existingEvent.get();
                        updateEventFromGoogle(personalHubEvent, googleEvent);
                        eventRepository.save(personalHubEvent);
                    } else {
                        // Create new event
                        com.zametech.todoapp.domain.model.Event personalHubEvent = 
                            googleCalendarService.convertFromGoogleEvent(googleEvent, userId);
                        personalHubEvent.setGoogleCalendarId(calendarId);
                        eventRepository.save(personalHubEvent);
                    }
                    
                    syncedEvents++;
                } catch (Exception e) {
                    log.error("Error syncing event {} from Google: {}", googleEvent.getId(), e.getMessage());
                    errorEvents++;
                }
            }
            
            return new SyncResult(totalEvents, syncedEvents, 0, errorEvents, now);
            
        } catch (Exception e) {
            log.error("Error syncing from Google Calendar {}: {}", calendarId, e.getMessage(), e);
            return new SyncResult(0, 0, 0, 1, null);
        }
    }

    /**
     * Sync events from Personal Hub to Google Calendar
     */
    private SyncResult syncToGoogle(String userCredentialsJson, String calendarId, Long userId) {
        try {
            // Get events that need to be synced to Google
            List<com.zametech.todoapp.domain.model.Event> eventsToSync = 
                eventRepository.findByUserIdAndSyncStatus(userId, "SYNC_PENDING");
            
            // Also get events that were modified since last sync
            List<com.zametech.todoapp.domain.model.Event> modifiedEvents = 
                eventRepository.findByUserIdAndLastSyncedAtAfter(userId, LocalDateTime.now().minusHours(1));
            
            // Combine lists (remove duplicates)
            eventsToSync.addAll(modifiedEvents.stream()
                .filter(event -> !eventsToSync.contains(event))
                .toList());
            
            int totalEvents = eventsToSync.size();
            int syncedEvents = 0;
            int errorEvents = 0;
            
            for (com.zametech.todoapp.domain.model.Event event : eventsToSync) {
                try {
                    if (event.getGoogleEventId() == null) {
                        // Create new event in Google Calendar
                        Optional<String> googleEventId = googleCalendarService.createCalendarEvent(
                            userCredentialsJson, calendarId, event
                        );
                        
                        if (googleEventId.isPresent()) {
                            event.setGoogleEventId(googleEventId.get());
                            event.setGoogleCalendarId(calendarId);
                            event.setSyncStatus("SYNCED");
                            event.setLastSyncedAt(LocalDateTime.now());
                            eventRepository.save(event);
                            syncedEvents++;
                        } else {
                            event.setSyncStatus("SYNC_ERROR");
                            eventRepository.save(event);
                            errorEvents++;
                        }
                    } else {
                        // Update existing event in Google Calendar
                        boolean success = googleCalendarService.updateCalendarEvent(
                            userCredentialsJson, calendarId, event.getGoogleEventId(), event
                        );
                        
                        if (success) {
                            event.setSyncStatus("SYNCED");
                            event.setLastSyncedAt(LocalDateTime.now());
                            eventRepository.save(event);
                            syncedEvents++;
                        } else {
                            event.setSyncStatus("SYNC_ERROR");
                            eventRepository.save(event);
                            errorEvents++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error syncing event {} to Google: {}", event.getId(), e.getMessage());
                    event.setSyncStatus("SYNC_ERROR");
                    eventRepository.save(event);
                    errorEvents++;
                }
            }
            
            return new SyncResult(totalEvents, syncedEvents, 0, errorEvents, LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error syncing to Google Calendar {}: {}", calendarId, e.getMessage(), e);
            return new SyncResult(0, 0, 0, 1, null);
        }
    }

    /**
     * Update Personal Hub event with data from Google event
     */
    private void updateEventFromGoogle(com.zametech.todoapp.domain.model.Event personalHubEvent, Event googleEvent) {
        personalHubEvent.setTitle(googleEvent.getSummary());
        personalHubEvent.setDescription(googleEvent.getDescription());
        personalHubEvent.setLocation(googleEvent.getLocation());
        personalHubEvent.setSyncStatus("SYNCED");
        personalHubEvent.setLastSyncedAt(LocalDateTime.now());
        
        // Update times if needed
        // Note: This is a simplified update, might need more sophisticated conflict resolution
    }

    /**
     * Get sync status for user
     */
    public CalendarSyncStatusResponse getSyncStatus() {
        Long userId = userContextService.getCurrentUserIdAsLong();
        
        List<CalendarSyncSettingsEntity> settings = calendarSyncSettingsRepository.findByUserId(userId);
        
        if (settings.isEmpty()) {
            return new CalendarSyncStatusResponse(
                false, null, "NOT_CONNECTED", List.of(), 
                new CalendarSyncStatusResponse.SyncStatistics(0, 0, 0, 0, null)
            );
        }
        
        // Calculate statistics
        List<com.zametech.todoapp.domain.model.Event> userEvents = 
            eventRepository.findByUserIdAndDateRange(userId, 
                LocalDateTime.now().minusDays(30), LocalDateTime.now().plusDays(365));
        
        int totalEvents = userEvents.size();
        int syncedEvents = (int) userEvents.stream().filter(e -> "SYNCED".equals(e.getSyncStatus())).count();
        int pendingEvents = (int) userEvents.stream().filter(e -> "SYNC_PENDING".equals(e.getSyncStatus())).count();
        int errorEvents = (int) userEvents.stream().filter(e -> "SYNC_ERROR".equals(e.getSyncStatus())).count();
        
        LocalDateTime lastSuccessfulSync = settings.stream()
            .map(CalendarSyncSettingsEntity::getLastSyncAt)
            .filter(date -> date != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        
        CalendarSyncStatusResponse.SyncStatistics stats = 
            new CalendarSyncStatusResponse.SyncStatistics(
                totalEvents, syncedEvents, pendingEvents, errorEvents, lastSuccessfulSync
            );
        
        return new CalendarSyncStatusResponse(
            true, lastSuccessfulSync, 
            errorEvents > 0 ? "ERROR" : "SUCCESS",
            settings.stream().map(this::toResponse).toList(),
            stats
        );
    }

    /**
     * Convert entity to response DTO
     */
    private com.zametech.todoapp.presentation.dto.response.CalendarSyncSettingsResponse toResponse(CalendarSyncSettingsEntity entity) {
        return new com.zametech.todoapp.presentation.dto.response.CalendarSyncSettingsResponse(
            entity.getId(),
            entity.getGoogleCalendarId(),
            entity.getCalendarName(),
            entity.getSyncEnabled(),
            entity.getLastSyncAt(),
            entity.getSyncDirection(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Result of sync operation
     */
    private record SyncResult(
        int totalEvents,
        int syncedEvents,
        int pendingEvents,
        int errorEvents,
        LocalDateTime lastSuccessfulSync
    ) {}
}