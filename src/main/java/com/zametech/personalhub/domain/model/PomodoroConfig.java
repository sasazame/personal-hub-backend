package com.zametech.personalhub.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing user's Pomodoro timer configuration.
 * Stores preferences like work duration, break duration, sound settings, etc.
 */
public class PomodoroConfig {
    private UUID id;
    private UUID userId;
    private Integer workDuration; // in minutes, default 25
    private Integer shortBreakDuration; // in minutes, default 5
    private Integer longBreakDuration; // in minutes, default 15
    private Integer cyclesBeforeLongBreak; // default 4
    private String alarmSound; // sound file identifier
    private Integer alarmVolume; // 0-100
    private Boolean autoStartBreaks;
    private Boolean autoStartWork;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public PomodoroConfig() {
        // Set defaults
        this.workDuration = 25;
        this.shortBreakDuration = 5;
        this.longBreakDuration = 15;
        this.cyclesBeforeLongBreak = 4;
        this.alarmSound = "default";
        this.alarmVolume = 50;
        this.autoStartBreaks = true;
        this.autoStartWork = false;
    }

    public PomodoroConfig(UUID userId) {
        this();
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getWorkDuration() {
        return workDuration;
    }

    public void setWorkDuration(Integer workDuration) {
        this.workDuration = workDuration;
    }

    public Integer getShortBreakDuration() {
        return shortBreakDuration;
    }

    public void setShortBreakDuration(Integer shortBreakDuration) {
        this.shortBreakDuration = shortBreakDuration;
    }

    public Integer getLongBreakDuration() {
        return longBreakDuration;
    }

    public void setLongBreakDuration(Integer longBreakDuration) {
        this.longBreakDuration = longBreakDuration;
    }

    public Integer getCyclesBeforeLongBreak() {
        return cyclesBeforeLongBreak;
    }

    public void setCyclesBeforeLongBreak(Integer cyclesBeforeLongBreak) {
        this.cyclesBeforeLongBreak = cyclesBeforeLongBreak;
    }

    public String getAlarmSound() {
        return alarmSound;
    }

    public void setAlarmSound(String alarmSound) {
        this.alarmSound = alarmSound;
    }

    public Integer getAlarmVolume() {
        return alarmVolume;
    }

    public void setAlarmVolume(Integer alarmVolume) {
        this.alarmVolume = alarmVolume;
    }

    public Boolean getAutoStartBreaks() {
        return autoStartBreaks;
    }

    public void setAutoStartBreaks(Boolean autoStartBreaks) {
        this.autoStartBreaks = autoStartBreaks;
    }

    public Boolean getAutoStartWork() {
        return autoStartWork;
    }

    public void setAutoStartWork(Boolean autoStartWork) {
        this.autoStartWork = autoStartWork;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}