package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.Goal;
import com.zametech.personalhub.domain.model.GoalType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository {
    Goal save(Goal goal);
    Optional<Goal> findById(Long id);
    List<Goal> findByUserId(UUID userId);
    List<Goal> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    List<Goal> findByUserIdAndGoalType(UUID userId, GoalType goalType);
    List<Goal> findByUserIdAndDateBetween(UUID userId, LocalDate date);
    void deleteById(Long id);
    boolean existsByIdAndUserId(Long id, UUID userId);
}