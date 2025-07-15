package com.zametech.personalhub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for Pomodoro configuration.
 */
@Entity
@Table(name = "pomodoro_configs")
public class PomodoroConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "work_duration", nullable = false)
    private Integer workDuration = 25;

    @Column(name = "short_break_duration", nullable = false)
    private Integer shortBreakDuration = 5;

    @Column(name = "long_break_duration", nullable = false)
    private Integer longBreakDuration = 15;

    @Column(name = "cycles_before_long_break", nullable = false)
    private Integer cyclesBeforeLongBreak = 4;

    @Column(name = "alarm_sound", nullable = false)
    private String alarmSound = "default";

    @Column(name = "alarm_volume", nullable = false)
    private Integer alarmVolume = 50;

    @Column(name = "auto_start_breaks", nullable = false)
    private Boolean autoStartBreaks = true;

    @Column(name = "auto_start_work", nullable = false)
    private Boolean autoStartWork = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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