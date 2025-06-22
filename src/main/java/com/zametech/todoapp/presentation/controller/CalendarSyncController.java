package com.zametech.todoapp.presentation.controller;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.zametech.todoapp.application.service.CalendarSyncService;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.todoapp.presentation.dto.request.CalendarSyncSettingsRequest;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncSettingsResponse;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/calendar/sync")
@RequiredArgsConstructor
@Slf4j
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;
    private final CalendarSyncSettingsRepository calendarSyncSettingsRepository;
    private final UserContextService userContextService;

    @PostMapping("/connect")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CalendarListEntry>> connectGoogleCalendar(
            @RequestBody String userCredentialsJson) {
        try {
            List<CalendarListEntry> calendars = calendarSyncService.connectGoogleCalendar(userCredentialsJson);
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error connecting Google Calendar: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/disconnect/{calendarId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> disconnectGoogleCalendar(@PathVariable String calendarId) {
        try {
            calendarSyncService.disconnectGoogleCalendar(calendarId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error disconnecting Google Calendar {}: {}", calendarId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/manual")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CalendarSyncStatusResponse> performManualSync(
            @RequestBody String userCredentialsJson) {
        try {
            CalendarSyncStatusResponse status = calendarSyncService.performSync(userCredentialsJson);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error performing manual sync: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CalendarSyncStatusResponse> getSyncStatus() {
        try {
            CalendarSyncStatusResponse status = calendarSyncService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting sync status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CalendarSyncSettingsResponse>> getSyncSettings() {
        try {
            Long userId = userContextService.getCurrentUserIdAsLong();
            List<CalendarSyncSettingsEntity> settings = calendarSyncSettingsRepository.findByUserId(userId);
            List<CalendarSyncSettingsResponse> responses = settings.stream()
                .map(setting -> new CalendarSyncSettingsResponse(
                    setting.getId(),
                    setting.getGoogleCalendarId(),
                    setting.getCalendarName(),
                    setting.getSyncEnabled(),
                    setting.getLastSyncAt(),
                    setting.getSyncDirection(),
                    setting.getCreatedAt(),
                    setting.getUpdatedAt()
                ))
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting sync settings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/settings/{calendarId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CalendarSyncSettingsResponse> updateSyncSettings(
            @PathVariable String calendarId,
            @Valid @RequestBody CalendarSyncSettingsRequest request) {
        try {
            // This would need implementation with userContextService
            // For now, returning not found as placeholder
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating sync settings for calendar {}: {}", calendarId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/auth/url")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> getAuthorizationUrl() {
        try {
            // This would return the OAuth URL for frontend to redirect to
            // Implementation depends on OAuth flow setup
            String authUrl = "https://accounts.google.com/oauth2/auth?..."; // Placeholder
            return ResponseEntity.ok(authUrl);
        } catch (Exception e) {
            log.error("Error generating authorization URL: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/auth/callback")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> handleOAuthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        try {
            // This would handle the OAuth callback and store tokens
            // Implementation depends on OAuth flow setup
            log.info("Received OAuth callback with code: {}", code);
            return ResponseEntity.ok("Authorization successful");
        } catch (Exception e) {
            log.error("Error handling OAuth callback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testConnection(@RequestBody String userCredentialsJson) {
        try {
            List<CalendarListEntry> calendars = calendarSyncService.connectGoogleCalendar(userCredentialsJson);
            return ResponseEntity.ok("Connection successful. Found " + calendars.size() + " calendars.");
        } catch (Exception e) {
            log.error("Error testing calendar connection: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Connection failed: " + e.getMessage());
        }
    }
}