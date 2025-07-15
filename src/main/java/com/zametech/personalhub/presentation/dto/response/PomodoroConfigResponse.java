package com.zametech.personalhub.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Pomodoro configuration data.
 */
public class PomodoroConfigResponse {
    private UUID id;
    private UUID userId;
    private Integer workDuration;
    private Integer shortBreakDuration;
    private Integer longBreakDuration;
    private Integer cyclesBeforeLongBreak;
    private String alarmSound;
    private Integer alarmVolume;
    private Boolean autoStartBreaks;
    private Boolean autoStartWork;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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