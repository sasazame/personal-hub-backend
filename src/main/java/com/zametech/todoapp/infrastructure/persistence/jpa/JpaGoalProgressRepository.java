package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.infrastructure.persistence.entity.GoalProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaGoalProgressRepository extends JpaRepository<GoalProgressEntity, Long> {
    Optional<GoalProgressEntity> findByGoalIdAndDate(Long goalId, LocalDate date);
    List<GoalProgressEntity> findByGoalId(Long goalId);
    
    @Query("SELECT p FROM GoalProgressEntity p WHERE p.goalId = :goalId " +
           "AND p.date BETWEEN :startDate AND :endDate ORDER BY p.date")
    List<GoalProgressEntity> findByGoalIdAndDateRange(@Param("goalId") Long goalId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM GoalProgressEntity p WHERE p.goalId = :goalId ORDER BY p.date DESC")
    List<GoalProgressEntity> findByGoalIdOrderByDateDesc(@Param("goalId") Long goalId);
    
    void deleteByGoalId(Long goalId);
}