package com.zametech.personalhub.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing a Pomodoro timer session.
 * A session consists of work periods and break periods, along with associated tasks.
 */
public class PomodoroSession {
    private UUID id;
    private UUID userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer workDuration; // in minutes
    private Integer breakDuration; // in minutes
    private Integer completedCycles;
    private SessionStatus status;
    private SessionType sessionType;
    private List<PomodoroTask> tasks = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED
    }

    public enum SessionType {
        WORK,
        SHORT_BREAK,
        LONG_BREAK
    }

    // Constructors
    public PomodoroSession() {}

    public PomodoroSession(UUID userId, Integer workDuration, Integer breakDuration) {
        this.userId = userId;
        this.workDuration = workDuration;
        this.breakDuration = breakDuration;
        this.status = SessionStatus.ACTIVE;
        this.sessionType = SessionType.WORK;
        this.completedCycles = 0;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getWorkDuration() {
        return workDuration;
    }

    public void setWorkDuration(Integer workDuration) {
        this.workDuration = workDuration;
    }

    public Integer getBreakDuration() {
        return breakDuration;
    }

    public void setBreakDuration(Integer breakDuration) {
        this.breakDuration = breakDuration;
    }

    public Integer getCompletedCycles() {
        return completedCycles;
    }

    public void setCompletedCycles(Integer completedCycles) {
        this.completedCycles = completedCycles;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public List<PomodoroTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<PomodoroTask> tasks) {
        this.tasks = tasks;
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