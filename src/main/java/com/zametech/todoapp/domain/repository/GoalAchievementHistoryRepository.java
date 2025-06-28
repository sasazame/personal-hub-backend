package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.GoalAchievementHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalAchievementHistoryRepository {
    GoalAchievementHistory save(GoalAchievementHistory achievement);
    Optional<GoalAchievementHistory> findByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate);
    List<GoalAchievementHistory> findByGoalId(Long goalId);
    List<GoalAchievementHistory> findByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate);
    void delete(GoalAchievementHistory achievement);
    void deleteByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate);
    long countByGoalId(Long goalId);
    long countByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate);
}