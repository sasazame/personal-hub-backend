package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.infrastructure.persistence.entity.GoalAchievementHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaGoalAchievementHistoryRepository extends JpaRepository<GoalAchievementHistoryEntity, Long> {
    Optional<GoalAchievementHistoryEntity> findByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate);
    List<GoalAchievementHistoryEntity> findByGoalId(Long goalId);
    List<GoalAchievementHistoryEntity> findByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate);
    void deleteByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate);
    long countByGoalId(Long goalId);
    long countByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate);
}