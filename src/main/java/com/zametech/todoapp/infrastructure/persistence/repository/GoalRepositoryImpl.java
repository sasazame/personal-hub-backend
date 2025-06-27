package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalStatus;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.repository.GoalRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.GoalEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GoalRepositoryImpl implements GoalRepository {
    private final JpaGoalRepository jpaGoalRepository;

    @Override
    public Goal save(Goal goal) {
        GoalEntity entity = toEntity(goal);
        GoalEntity saved = jpaGoalRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Goal> findById(Long id) {
        return jpaGoalRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Goal> findByUserId(UUID userId) {
        return jpaGoalRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Goal> findByUserIdAndStatus(UUID userId, GoalStatus status) {
        return jpaGoalRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Goal> findByUserIdAndGoalType(UUID userId, GoalType goalType) {
        return jpaGoalRepository.findByUserIdAndGoalType(userId, goalType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Goal> findActiveGoalsByUserIdAndDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        return jpaGoalRepository.findActiveGoalsByUserIdAndDateRange(userId, startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaGoalRepository.deleteById(id);
    }

    @Override
    public boolean existsByIdAndUserId(Long id, UUID userId) {
        return jpaGoalRepository.existsByIdAndUserId(id, userId);
    }

    private Goal toDomain(GoalEntity entity) {
        Goal goal = new Goal();
        goal.setId(entity.getId());
        goal.setUserId(entity.getUserId());
        goal.setTitle(entity.getTitle());
        goal.setDescription(entity.getDescription());
        goal.setGoalType(entity.getGoalType());
        goal.setMetricType(entity.getMetricType());
        goal.setMetricUnit(entity.getMetricUnit());
        goal.setTargetValue(entity.getTargetValue());
        goal.setCurrentValue(entity.getCurrentValue());
        goal.setStartDate(entity.getStartDate());
        goal.setEndDate(entity.getEndDate());
        goal.setStatus(entity.getStatus());
        goal.setCreatedAt(entity.getCreatedAt());
        goal.setUpdatedAt(entity.getUpdatedAt());
        return goal;
    }

    private GoalEntity toEntity(Goal goal) {
        GoalEntity entity = new GoalEntity();
        entity.setId(goal.getId());
        entity.setUserId(goal.getUserId());
        entity.setTitle(goal.getTitle());
        entity.setDescription(goal.getDescription());
        entity.setGoalType(goal.getGoalType());
        entity.setMetricType(goal.getMetricType());
        entity.setMetricUnit(goal.getMetricUnit());
        entity.setTargetValue(goal.getTargetValue());
        entity.setCurrentValue(goal.getCurrentValue());
        entity.setStartDate(goal.getStartDate());
        entity.setEndDate(goal.getEndDate());
        entity.setStatus(goal.getStatus());
        entity.setCreatedAt(goal.getCreatedAt());
        entity.setUpdatedAt(goal.getUpdatedAt());
        return entity;
    }
}