package com.zametech.personalhub.presentation.controller;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.zametech.personalhub.application.service.CalendarSyncService;
import com.zametech.personalhub.application.service.UserContextService;
import com.zametech.personalhub.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.personalhub.presentation.dto.request.CalendarSyncSettingsRequest;
import com.zametech.personalhub.presentation.dto.request.GoogleSyncSettingsRequest;
import com.zametech.personalhub.presentation.dto.response.CalendarSyncSettingsResponse;
import com.zametech.personalhub.presentation.dto.response.CalendarSyncStatusResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncSettingsResponse;
import com.zametech.personalhub.presentation.dto.response.GoogleSyncStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public ResponseEntity<List<CalendarListEntry>> connectGoogleCalendar() {
        try {
            List<CalendarListEntry> calendars = calendarSyncService.connectGoogleCalendar();
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

    @PostMapping("/manual/detailed")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CalendarSyncStatusResponse> performManualSync() {
        try {
            CalendarSyncStatusResponse status = calendarSyncService.performSync();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error performing manual sync: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/detailed")
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

    @GetMapping("/settings/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CalendarSyncSettingsResponse>> getSyncSettings() {
        try {
            UUID userId = userContextService.getCurrentUserId();
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

    @PutMapping("/settings/calendar/{calendarId}")
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
    public ResponseEntity<String> testConnection() {
        try {
            List<CalendarListEntry> calendars = calendarSyncService.connectGoogleCalendar();
            return ResponseEntity.ok("Connection successful. Found " + calendars.size() + " calendars.");
        } catch (Exception e) {
            log.error("Error testing calendar connection: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Connection failed: " + e.getMessage());
        }
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GoogleSyncSettingsResponse> getGoogleSyncSettings() {
        try {
            GoogleSyncSettingsResponse settings = calendarSyncService.getGoogleSyncSettings();
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error getting Google sync settings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GoogleSyncSettingsResponse> updateGoogleSyncSettings(
            @Valid @RequestBody GoogleSyncSettingsRequest request) {
        try {
            GoogleSyncSettingsResponse settings = calendarSyncService.updateGoogleSyncSettings(request);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error updating Google sync settings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GoogleSyncStatusResponse> triggerManualSync() {
        try {
            GoogleSyncStatusResponse status = calendarSyncService.triggerManualSync();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error triggering manual sync: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<GoogleSyncStatusResponse> getGoogleSyncStatus() {
        try {
            GoogleSyncStatusResponse status = calendarSyncService.getGoogleSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting Google sync status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}