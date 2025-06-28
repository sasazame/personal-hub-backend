package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository {
    Goal save(Goal goal);
    Optional<Goal> findById(String id);
    List<Goal> findByUserId(UUID userId);
    List<Goal> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    List<Goal> findByUserIdAndGoalType(UUID userId, GoalType goalType);
    List<Goal> findByUserIdAndDateBetween(UUID userId, LocalDate date);
    void deleteById(String id);
    boolean existsByIdAndUserId(String id, UUID userId);
}