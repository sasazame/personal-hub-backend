package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.GoalAchievementHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalAchievementHistoryRepository {
    GoalAchievementHistory save(GoalAchievementHistory achievement);
    Optional<GoalAchievementHistory> findByGoalIdAndAchievedDate(String goalId, LocalDate achievedDate);
    List<GoalAchievementHistory> findByGoalId(String goalId);
    List<GoalAchievementHistory> findByGoalIdAndAchievedDateBetween(String goalId, LocalDate startDate, LocalDate endDate);
    void delete(GoalAchievementHistory achievement);
    void deleteByGoalIdAndAchievedDate(String goalId, LocalDate achievedDate);
    long countByGoalId(String goalId);
    long countByGoalIdAndAchievedDateBetween(String goalId, LocalDate startDate, LocalDate endDate);
}