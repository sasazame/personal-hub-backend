package com.zametech.todoapp.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "calendar_sync_settings")
public class CalendarSyncSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "google_calendar_id", nullable = false)
    private String googleCalendarId;

    @Column(name = "calendar_name")
    private String calendarName;

    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "sync_direction", length = 20, nullable = false)
    private String syncDirection = "BIDIRECTIONAL";

    @Column(name = "auto_sync", nullable = false)
    private Boolean autoSync = true;

    @Column(name = "sync_interval", nullable = false)
    private Integer syncInterval = 30;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CalendarSyncSettingsEntity(UUID userId, String googleCalendarId, String calendarName) {
        this.userId = userId;
        this.googleCalendarId = googleCalendarId;
        this.calendarName = calendarName;
        this.syncEnabled = true;
        this.syncDirection = "BIDIRECTIONAL";
        this.autoSync = true;
        this.syncInterval = 30;
    }
}