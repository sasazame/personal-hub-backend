package com.zametech.todoapp.application.service;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.domain.repository.UserRepository;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import com.zametech.todoapp.presentation.dto.response.GoogleSyncSettingsResponse;
import com.zametech.todoapp.presentation.dto.response.GoogleSyncStatusResponse;
import com.zametech.todoapp.presentation.dto.request.GoogleSyncSettingsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.zametech.todoapp.common.exception.OAuth2RequiredException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncService {

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarOAuth2Service googleCalendarOAuth2Service;
    private final EventRepository eventRepository;
    private final CalendarSyncSettingsRepository calendarSyncSettingsRepository;
    private final UserContextService userContextService;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;

    /**
     * Connect user to Google Calendar using OAuth2
     */
    @Transactional
    public List<CalendarListEntry> connectGoogleCalendar() {
        UUID userId = userContextService.getCurrentUserId();
        
        try {
            // Get user entity
            com.zametech.todoapp.domain.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            // Check if user has connected Google account
            Optional<UserSocialAccount> socialAccountOpt = socialAccountRepository
                .findByUserIdAndProvider(user.getId(), "google");
            
            if (socialAccountOpt.isEmpty()) {
                throw new OAuth2RequiredException("Please login with Google OAuth2 first to connect calendars");
            }
            
            // Use OAuth2 service to get calendars
            List<CalendarListEntry> calendars = googleCalendarOAuth2Service.getUserCalendars(user);
            
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
        } catch (OAuth2RequiredException e) {
            log.warn("User {} attempted to connect calendars without OAuth2: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error connecting Google Calendar for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to connect Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnect user from Google Calendar
     */
    @Transactional
    public void disconnectGoogleCalendar(String calendarId) {
        UUID userId = userContextService.getCurrentUserId();
        
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
    public CalendarSyncStatusResponse performSync() {
        UUID userId = userContextService.getCurrentUserId();
        
        try {
            // Get user entity
            com.zametech.todoapp.domain.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            List<CalendarSyncSettingsEntity> settings = 
                calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId);
            
            int totalEvents = 0;
            int syncedEvents = 0;
            int pendingEvents = 0;
            int errorEvents = 0;
            LocalDateTime lastSuccessfulSync = null;
            
            for (CalendarSyncSettingsEntity setting : settings) {
                SyncResult result = syncCalendar(user, setting);
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
    private SyncResult syncCalendar(com.zametech.todoapp.domain.model.User user, CalendarSyncSettingsEntity setting) {
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
                SyncResult fromGoogleResult = syncFromGoogle(user, calendarId, lastSyncAt);
                totalEvents += fromGoogleResult.totalEvents();
                syncedEvents += fromGoogleResult.syncedEvents();
                errorEvents += fromGoogleResult.errorEvents();
            }
            
            // Sync from Personal Hub to Google Calendar
            if ("BIDIRECTIONAL".equals(syncDirection) || "TO_GOOGLE".equals(syncDirection)) {
                SyncResult toGoogleResult = syncToGoogle(user, calendarId, setting.getUserId());
                totalEvents += toGoogleResult.totalEvents();
                syncedEvents += toGoogleResult.syncedEvents();
                errorEvents += toGoogleResult.errorEvents();
            }
            
            return new SyncResult(
                totalEvents, syncedEvents, pendingEvents, errorEvents, LocalDateTime.now()
            );
            
        } catch (IllegalStateException e) {
            log.error("Authentication error syncing calendar {}: {}", setting.getGoogleCalendarId(), e.getMessage());
            return new SyncResult(0, 0, 0, 1, null);
        } catch (Exception e) {
            log.error("Error syncing calendar {}: {}", setting.getGoogleCalendarId(), e.getMessage(), e);
            return new SyncResult(0, 0, 0, 1, null);
        }
    }

    /**
     * Sync events from Google Calendar to Personal Hub
     */
    private SyncResult syncFromGoogle(com.zametech.todoapp.domain.model.User user, String calendarId, LocalDateTime lastSyncAt) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> googleEvents = googleCalendarOAuth2Service.getCalendarEvents(
                user, calendarId, lastSyncAt, now.plusDays(365)
            );
            
            int totalEvents = googleEvents.size();
            int syncedEvents = 0;
            int errorEvents = 0;
            
            UUID userId = userContextService.getCurrentUserId();
            
            for (Event googleEvent : googleEvents) {
                try {
                    // 繰り返しイベントのインスタンスかチェック
                    String eventIdToCheck = googleEvent.getId();
                    if (googleEvent.getRecurringEventId() != null) {
                        eventIdToCheck = googleEvent.getRecurringEventId();
                        log.debug("Processing recurring event instance: {} (master: {})", 
                            googleEvent.getId(), eventIdToCheck);
                    }
                    
                    // Check if event already exists
                    Optional<com.zametech.todoapp.domain.model.Event> existingEvent = 
                        eventRepository.findByGoogleEventId(eventIdToCheck);
                    
                    if (existingEvent.isPresent()) {
                        // Update existing event
                        com.zametech.todoapp.domain.model.Event personalHubEvent = existingEvent.get();
                        updateEventFromGoogle(personalHubEvent, googleEvent);
                        eventRepository.save(personalHubEvent);
                    } else {
                        // Create new event
                        com.zametech.todoapp.domain.model.Event personalHubEvent = 
                            convertFromGoogleEvent(googleEvent, userId);
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
    private SyncResult syncToGoogle(com.zametech.todoapp.domain.model.User user, String calendarId, UUID userId) {
        try {
            // Get events that need to be synced to Google
            List<com.zametech.todoapp.domain.model.Event> pendingEvents = 
                eventRepository.findByUserIdAndSyncStatus(userId, "SYNC_PENDING");
            
            // Also get events that were modified since last sync
            List<com.zametech.todoapp.domain.model.Event> modifiedEvents = 
                eventRepository.findByUserIdAndLastSyncedAtAfter(userId, LocalDateTime.now().minusHours(1));
            
            // Combine lists (remove duplicates) - create a new mutable list
            List<com.zametech.todoapp.domain.model.Event> eventsToSync = new ArrayList<>(pendingEvents);
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
                        Optional<String> googleEventId = googleCalendarOAuth2Service.createCalendarEvent(
                            user, calendarId, event
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
                        boolean success = googleCalendarOAuth2Service.updateCalendarEvent(
                            user, calendarId, event.getGoogleEventId(), event
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
        personalHubEvent.setTitle(googleEvent.getSummary() != null ? googleEvent.getSummary() : "Untitled Event");
        personalHubEvent.setDescription(googleEvent.getDescription());
        personalHubEvent.setLocation(googleEvent.getLocation());
        personalHubEvent.setSyncStatus("SYNCED");
        personalHubEvent.setLastSyncedAt(LocalDateTime.now());
        
        // Update times
        if (googleEvent.getStart() != null) {
            if (googleEvent.getStart().getDateTime() != null) {
                personalHubEvent.setStartDateTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
                    java.time.ZoneId.systemDefault()
                ));
                personalHubEvent.setAllDay(false);
            } else if (googleEvent.getStart().getDate() != null) {
                String dateStr = googleEvent.getStart().getDate().toStringRfc3339();
                personalHubEvent.setStartDateTime(LocalDateTime.parse(dateStr + "T00:00:00"));
                personalHubEvent.setAllDay(true);
            }
        }
        
        if (googleEvent.getEnd() != null) {
            if (googleEvent.getEnd().getDateTime() != null) {
                personalHubEvent.setEndDateTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue()),
                    java.time.ZoneId.systemDefault()
                ));
            } else if (googleEvent.getEnd().getDate() != null) {
                String dateStr = googleEvent.getEnd().getDate().toStringRfc3339();
                LocalDateTime endDate = LocalDateTime.parse(dateStr + "T00:00:00");
                personalHubEvent.setEndDateTime(endDate.minusDays(1).withHour(23).withMinute(59).withSecond(59));
            }
        }
        
        // Update color
        if (googleEvent.getColorId() != null) {
            personalHubEvent.setColor(mapGoogleColorToHex(googleEvent.getColorId()));
        }
    }

    /**
     * Get sync status for user
     */
    public CalendarSyncStatusResponse getSyncStatus() {
        UUID userId = userContextService.getCurrentUserId();
        
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
     * Get sync settings for frontend
     */
    public GoogleSyncSettingsResponse getGoogleSyncSettings() {
        UUID userId = userContextService.getCurrentUserId();
        
        List<CalendarSyncSettingsEntity> settings = calendarSyncSettingsRepository.findByUserId(userId);
        
        if (settings.isEmpty()) {
            return new GoogleSyncSettingsResponse(false, null, "BIDIRECTIONAL", false, 30);
        }
        
        CalendarSyncSettingsEntity primarySetting = settings.get(0);
        
        return new GoogleSyncSettingsResponse(
            primarySetting.getSyncEnabled(),
            primarySetting.getGoogleCalendarId(),
            primarySetting.getSyncDirection(),
            primarySetting.getAutoSync(),
            primarySetting.getSyncInterval()
        );
    }

    /**
     * Update sync settings from frontend
     */
    @Transactional
    public GoogleSyncSettingsResponse updateGoogleSyncSettings(GoogleSyncSettingsRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        CalendarSyncSettingsEntity setting;
        
        if (request.calendarId() != null) {
            Optional<CalendarSyncSettingsEntity> existingSetting = 
                calendarSyncSettingsRepository.findByUserIdAndGoogleCalendarId(userId, request.calendarId());
            
            if (existingSetting.isPresent()) {
                setting = existingSetting.get();
            } else {
                setting = new CalendarSyncSettingsEntity(userId, request.calendarId(), "Primary Calendar");
            }
        } else {
            List<CalendarSyncSettingsEntity> userSettings = calendarSyncSettingsRepository.findByUserId(userId);
            if (userSettings.isEmpty()) {
                throw new RuntimeException("No calendar configured. Please set calendarId first.");
            }
            setting = userSettings.get(0);
        }
        
        setting.setSyncEnabled(request.enabled());
        setting.setSyncDirection(request.syncDirection());
        setting.setAutoSync(request.autoSync());
        setting.setSyncInterval(request.syncInterval());
        
        CalendarSyncSettingsEntity savedSetting = calendarSyncSettingsRepository.save(setting);
        
        return new GoogleSyncSettingsResponse(
            savedSetting.getSyncEnabled(),
            savedSetting.getGoogleCalendarId(),
            savedSetting.getSyncDirection(),
            savedSetting.getAutoSync(),
            savedSetting.getSyncInterval()
        );
    }

    /**
     * Get sync status for frontend
     */
    public GoogleSyncStatusResponse getGoogleSyncStatus() {
        UUID userId = userContextService.getCurrentUserId();
        
        List<CalendarSyncSettingsEntity> settings = calendarSyncSettingsRepository.findByUserId(userId);
        
        if (settings.isEmpty()) {
            return new GoogleSyncStatusResponse(null, null, false, 0, List.of());
        }
        
        CalendarSyncSettingsEntity setting = settings.get(0);
        LocalDateTime lastSyncAt = setting.getLastSyncAt();
        LocalDateTime nextSyncTime = null;
        
        if (setting.getAutoSync() && lastSyncAt != null) {
            nextSyncTime = lastSyncAt.plusMinutes(setting.getSyncInterval());
        }
        
        List<com.zametech.todoapp.domain.model.Event> userEvents = 
            eventRepository.findByUserIdAndDateRange(userId, 
                LocalDateTime.now().minusDays(30), LocalDateTime.now().plusDays(365));
        
        int syncedEvents = (int) userEvents.stream()
            .filter(e -> "SYNCED".equals(e.getSyncStatus()))
            .count();
        
        List<String> errors = userEvents.stream()
            .filter(e -> "SYNC_ERROR".equals(e.getSyncStatus()))
            .map(e -> "Failed to sync event: " + e.getTitle())
            .toList();
        
        return new GoogleSyncStatusResponse(
            lastSyncAt != null ? lastSyncAt.toString() : null,
            nextSyncTime != null ? nextSyncTime.toString() : null,
            false,
            syncedEvents,
            errors
        );
    }

    /**
     * Trigger manual sync for frontend
     */
    @Transactional
    public GoogleSyncStatusResponse triggerManualSync() {
        UUID userId = userContextService.getCurrentUserId();
        
        List<CalendarSyncSettingsEntity> settings = 
            calendarSyncSettingsRepository.findByUserIdAndSyncEnabledTrue(userId);
        
        if (settings.isEmpty()) {
            // If no settings exist, return mock response
            log.warn("No calendar sync settings found for user {}. Returning mock response.", userId);
            return new GoogleSyncStatusResponse(
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMinutes(30).toString(),
                false,
                0,
                List.of("No calendar configured")
            );
        }
        
        try {
            CalendarSyncStatusResponse syncResult = performSync();
            
            CalendarSyncSettingsEntity setting = settings.get(0);
            LocalDateTime lastSyncAt = setting.getLastSyncAt();
            LocalDateTime nextSyncTime = null;
            
            if (setting.getAutoSync() && lastSyncAt != null) {
                nextSyncTime = lastSyncAt.plusMinutes(setting.getSyncInterval());
            }
            
            return new GoogleSyncStatusResponse(
                lastSyncAt != null ? lastSyncAt.toString() : null,
                nextSyncTime != null ? nextSyncTime.toString() : null,
                false,
                syncResult.syncStatistics().syncedEvents(),
                List.of()
            );
            
        } catch (Exception e) {
            log.error("Manual sync failed: {}", e.getMessage(), e);
            return new GoogleSyncStatusResponse(
                null, null, false, 0, List.of("Manual sync failed: " + e.getMessage())
            );
        }
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
    
    /**
     * Convert Google Calendar event to Personal Hub event
     */
    private com.zametech.todoapp.domain.model.Event convertFromGoogleEvent(Event googleEvent, UUID userId) {
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();
        event.setUserId(userId);
        event.setTitle(googleEvent.getSummary() != null ? googleEvent.getSummary() : "Untitled Event");
        event.setDescription(googleEvent.getDescription());
        event.setLocation(googleEvent.getLocation());
        event.setGoogleEventId(googleEvent.getId());
        event.setSyncStatus("SYNCED");
        event.setLastSyncedAt(LocalDateTime.now());
        
        // Convert start time
        if (googleEvent.getStart() != null) {
            if (googleEvent.getStart().getDateTime() != null) {
                // 時刻指定イベント
                event.setStartDateTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
                    java.time.ZoneId.systemDefault()
                ));
                event.setAllDay(false);
            } else if (googleEvent.getStart().getDate() != null) {
                // 全日イベント
                String dateStr = googleEvent.getStart().getDate().toStringRfc3339();
                // RFC3339形式の日付（YYYY-MM-DD）をパース
                event.setStartDateTime(LocalDateTime.parse(dateStr + "T00:00:00"));
                event.setAllDay(true);
            }
        }
        
        // Convert end time
        if (googleEvent.getEnd() != null) {
            if (googleEvent.getEnd().getDateTime() != null) {
                // 時刻指定イベント
                event.setEndDateTime(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue()),
                    java.time.ZoneId.systemDefault()
                ));
            } else if (googleEvent.getEnd().getDate() != null) {
                // 全日イベント
                String dateStr = googleEvent.getEnd().getDate().toStringRfc3339();
                // Google Calendarの全日イベントは終了日が「翌日」を指すため、1日引く
                LocalDateTime endDate = LocalDateTime.parse(dateStr + "T00:00:00");
                event.setEndDateTime(endDate.minusDays(1).withHour(23).withMinute(59).withSecond(59));
            }
        }
        
        // Extract reminder information
        if (googleEvent.getReminders() != null && googleEvent.getReminders().getOverrides() != null 
            && !googleEvent.getReminders().getOverrides().isEmpty()) {
            event.setReminderMinutes(googleEvent.getReminders().getOverrides().get(0).getMinutes());
        }
        
        // Set color if available
        if (googleEvent.getColorId() != null) {
            event.setColor(mapGoogleColorToHex(googleEvent.getColorId()));
        } else {
            // デフォルトの色を設定
            event.setColor("#039BE5"); // Default blue
        }
        
        // 繰り返しイベントの処理
        if (googleEvent.getRecurringEventId() != null) {
            // 繰り返しイベントのインスタンスの場合
            event.setGoogleEventId(googleEvent.getRecurringEventId()); // マスターイベントIDを使用
            log.debug("Recurring event instance detected: {} -> master: {}", 
                googleEvent.getId(), googleEvent.getRecurringEventId());
        }
        
        // Status
        if ("cancelled".equals(googleEvent.getStatus())) {
            event.setSyncStatus("CANCELLED");
        }
        
        return event;
    }
    
    /**
     * Map Google Calendar color ID to hex color
     */
    private String mapGoogleColorToHex(String colorId) {
        // Google Calendar color mapping
        return switch (colorId) {
            case "1" -> "#7986CB";  // Lavender
            case "2" -> "#33B679";  // Sage
            case "3" -> "#8E24AA";  // Grape
            case "4" -> "#E67C73";  // Flamingo
            case "5" -> "#F6BF26";  // Banana
            case "6" -> "#F4511E";  // Tangerine
            case "7" -> "#039BE5";  // Peacock
            case "8" -> "#616161";  // Graphite
            case "9" -> "#3F51B5";  // Blueberry
            case "10" -> "#0B8043"; // Basil
            case "11" -> "#D50000"; // Tomato
            default -> "#039BE5";   // Default blue
        };
    }
}