package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;
    private boolean allDay;
    private Integer reminderMinutes;
    private String color;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Google Calendar sync fields
    private String googleCalendarId;
    private String googleEventId;
    private LocalDateTime lastSyncedAt;
    private String syncStatus;

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
        this.updatedAt = LocalDateTime.now();
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
        this.updatedAt = LocalDateTime.now();
    }

    public void setColor(String color) {
        this.color = color;
        this.updatedAt = LocalDateTime.now();
    }
}