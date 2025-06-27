package com.zametech.todoapp.domain.goal.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GoalStreak {
    private Long id;
    private Long goalId;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastAchievedDate;
    private LocalDate streakBrokenDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GoalStreak() {}

    public GoalStreak(Long goalId) {
        this.goalId = goalId;
        this.currentStreak = 0;
        this.longestStreak = 0;
    }

    public void incrementStreak() {
        currentStreak++;
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        lastAchievedDate = LocalDate.now();
    }

    public void resetStreak() {
        currentStreak = 0;
        streakBrokenDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDate getLastAchievedDate() {
        return lastAchievedDate;
    }

    public void setLastAchievedDate(LocalDate lastAchievedDate) {
        this.lastAchievedDate = lastAchievedDate;
    }

    public LocalDate getStreakBrokenDate() {
        return streakBrokenDate;
    }

    public void setStreakBrokenDate(LocalDate streakBrokenDate) {
        this.streakBrokenDate = streakBrokenDate;
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