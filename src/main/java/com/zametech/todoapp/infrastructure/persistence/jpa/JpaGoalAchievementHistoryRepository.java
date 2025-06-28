package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.infrastructure.persistence.entity.GoalAchievementHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaGoalAchievementHistoryRepository extends JpaRepository<GoalAchievementHistoryEntity, String> {
    Optional<GoalAchievementHistoryEntity> findByGoalIdAndAchievedDate(String goalId, LocalDate achievedDate);
    List<GoalAchievementHistoryEntity> findByGoalId(String goalId);
    List<GoalAchievementHistoryEntity> findByGoalIdAndAchievedDateBetween(String goalId, LocalDate startDate, LocalDate endDate);
    void deleteByGoalIdAndAchievedDate(String goalId, LocalDate achievedDate);
    long countByGoalId(String goalId);
    long countByGoalIdAndAchievedDateBetween(String goalId, LocalDate startDate, LocalDate endDate);
}