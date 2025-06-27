package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.GoalProgress;
import com.zametech.todoapp.domain.repository.GoalProgressRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.GoalProgressEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaGoalProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GoalProgressRepositoryImpl implements GoalProgressRepository {
    private final JpaGoalProgressRepository jpaGoalProgressRepository;

    @Override
    public GoalProgress save(GoalProgress progress) {
        GoalProgressEntity entity = toEntity(progress);
        GoalProgressEntity saved = jpaGoalProgressRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<GoalProgress> findById(Long id) {
        return jpaGoalProgressRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<GoalProgress> findByGoalIdAndDate(Long goalId, LocalDate date) {
        return jpaGoalProgressRepository.findByGoalIdAndDate(goalId, date).map(this::toDomain);
    }

    @Override
    public List<GoalProgress> findByGoalId(Long goalId) {
        return jpaGoalProgressRepository.findByGoalId(goalId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalProgress> findByGoalIdAndDateRange(Long goalId, LocalDate startDate, LocalDate endDate) {
        return jpaGoalProgressRepository.findByGoalIdAndDateRange(goalId, startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalProgress> findByGoalIdOrderByDateDesc(Long goalId) {
        return jpaGoalProgressRepository.findByGoalIdOrderByDateDesc(goalId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaGoalProgressRepository.deleteById(id);
    }

    @Override
    public void deleteByGoalId(Long goalId) {
        jpaGoalProgressRepository.deleteByGoalId(goalId);
    }

    private GoalProgress toDomain(GoalProgressEntity entity) {
        GoalProgress progress = new GoalProgress();
        progress.setId(entity.getId());
        progress.setGoalId(entity.getGoalId());
        progress.setDate(entity.getDate());
        progress.setValue(entity.getValue());
        progress.setNote(entity.getNote());
        progress.setCreatedAt(entity.getCreatedAt());
        progress.setUpdatedAt(entity.getUpdatedAt());
        return progress;
    }

    private GoalProgressEntity toEntity(GoalProgress progress) {
        GoalProgressEntity entity = new GoalProgressEntity();
        entity.setId(progress.getId());
        entity.setGoalId(progress.getGoalId());
        entity.setDate(progress.getDate());
        entity.setValue(progress.getValue());
        entity.setNote(progress.getNote());
        entity.setCreatedAt(progress.getCreatedAt());
        entity.setUpdatedAt(progress.getUpdatedAt());
        return entity;
    }
}