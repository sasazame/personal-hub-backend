package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalStatus;
import com.zametech.todoapp.domain.model.GoalType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository {
    Goal save(Goal goal);
    Optional<Goal> findById(Long id);
    List<Goal> findByUserId(UUID userId);
    List<Goal> findByUserIdAndStatus(UUID userId, GoalStatus status);
    List<Goal> findByUserIdAndGoalType(UUID userId, GoalType goalType);
    List<Goal> findActiveGoalsByUserIdAndDateRange(UUID userId, LocalDate startDate, LocalDate endDate);
    void deleteById(Long id);
    boolean existsByIdAndUserId(Long id, UUID userId);
}