package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.GoalProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GoalProgressRepository {
    GoalProgress save(GoalProgress progress);
    Optional<GoalProgress> findById(Long id);
    Optional<GoalProgress> findByGoalIdAndDate(Long goalId, LocalDate date);
    List<GoalProgress> findByGoalId(Long goalId);
    List<GoalProgress> findByGoalIdAndDateRange(Long goalId, LocalDate startDate, LocalDate endDate);
    List<GoalProgress> findByGoalIdOrderByDateDesc(Long goalId);
    void deleteById(Long id);
    void deleteByGoalId(Long goalId);
}