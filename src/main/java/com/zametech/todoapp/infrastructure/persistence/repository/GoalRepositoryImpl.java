package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.Goal;
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
    public Optional<Goal> findById(String id) {
        return jpaGoalRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Goal> findByUserId(UUID userId) {
        return jpaGoalRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Goal> findByUserIdAndIsActive(UUID userId, Boolean isActive) {
        return jpaGoalRepository.findByUserIdAndIsActive(userId, isActive).stream()
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
    public List<Goal> findByUserIdAndDateBetween(UUID userId, LocalDate date) {
        return jpaGoalRepository.findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(userId, date, date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaGoalRepository.deleteById(id);
    }

    @Override
    public boolean existsByIdAndUserId(String id, UUID userId) {
        return jpaGoalRepository.existsByIdAndUserId(id, userId);
    }

    private Goal toDomain(GoalEntity entity) {
        Goal goal = new Goal();
        goal.setId(entity.getId());
        goal.setUserId(entity.getUserId());
        goal.setTitle(entity.getTitle());
        goal.setDescription(entity.getDescription());
        goal.setGoalType(entity.getGoalType());
        goal.setIsActive(entity.getIsActive());
        goal.setStartDate(entity.getStartDate());
        goal.setEndDate(entity.getEndDate());
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
        entity.setIsActive(goal.getIsActive());
        entity.setStartDate(goal.getStartDate());
        entity.setEndDate(goal.getEndDate());
        entity.setCreatedAt(goal.getCreatedAt());
        entity.setUpdatedAt(goal.getUpdatedAt());
        return entity;
    }
}