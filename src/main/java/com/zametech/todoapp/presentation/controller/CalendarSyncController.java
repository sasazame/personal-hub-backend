package com.zametech.todoapp.presentation.controller;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.zametech.todoapp.application.service.CalendarSyncService;
import com.zametech.todoapp.domain.repository.CalendarSyncSettingsRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import com.zametech.todoapp.presentation.dto.request.CalendarSyncSettingsRequest;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncSettingsResponse;
import com.zametech.todoapp.presentation.dto.response.CalendarSyncStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Calendar Sync", description = "Google Calendar sync management")
public class CalendarSyncController {

    private final CalendarSyncService calendarSyncService;
    private final CalendarSyncSettingsRepository calendarSyncSettingsRepository;

    @PostMapping("/connect")
    @Operation(summary = "Connect to Google Calendar", 
               description = "Connect user account to Google Calendar and retrieve available calendars")
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
    @Operation(summary = "Disconnect from Google Calendar", 
               description = "Disconnect specific calendar from sync")
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
    @Operation(summary = "Perform manual sync", 
               description = "Manually trigger bidirectional sync with Google Calendar")
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
    @Operation(summary = "Get sync status", 
               description = "Get current sync status and statistics")
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
    @Operation(summary = "Get sync settings", 
               description = "Get all calendar sync settings for current user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CalendarSyncSettingsResponse>> getSyncSettings() {
        try {
            // This would need userContextService to get current user ID
            // For now, returning empty list as placeholder
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("Error getting sync settings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/settings/{calendarId}")
    @Operation(summary = "Update sync settings", 
               description = "Update sync settings for specific calendar")
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
    @Operation(summary = "Get OAuth authorization URL", 
               description = "Get Google OAuth2 authorization URL for calendar access")
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
    @Operation(summary = "Handle OAuth callback", 
               description = "Handle OAuth2 callback and exchange code for tokens")
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
    @Operation(summary = "Test calendar connection", 
               description = "Test connection to Google Calendar API")
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