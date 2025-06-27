package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.GoalMilestone;
import com.zametech.todoapp.domain.repository.GoalMilestoneRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.GoalMilestoneEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaGoalMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GoalMilestoneRepositoryImpl implements GoalMilestoneRepository {
    private final JpaGoalMilestoneRepository jpaGoalMilestoneRepository;

    @Override
    public GoalMilestone save(GoalMilestone milestone) {
        GoalMilestoneEntity entity = toEntity(milestone);
        GoalMilestoneEntity saved = jpaGoalMilestoneRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<GoalMilestone> findById(Long id) {
        return jpaGoalMilestoneRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<GoalMilestone> findByGoalId(Long goalId) {
        return jpaGoalMilestoneRepository.findByGoalId(goalId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalMilestone> findByGoalIdAndAchieved(Long goalId, Boolean achieved) {
        return jpaGoalMilestoneRepository.findByGoalIdAndAchieved(goalId, achieved).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaGoalMilestoneRepository.deleteById(id);
    }

    @Override
    public void deleteByGoalId(Long goalId) {
        jpaGoalMilestoneRepository.deleteByGoalId(goalId);
    }

    private GoalMilestone toDomain(GoalMilestoneEntity entity) {
        GoalMilestone milestone = new GoalMilestone();
        milestone.setId(entity.getId());
        milestone.setGoalId(entity.getGoalId());
        milestone.setTitle(entity.getTitle());
        milestone.setTargetValue(entity.getTargetValue());
        milestone.setAchieved(entity.getAchieved());
        milestone.setAchievedDate(entity.getAchievedDate());
        milestone.setCreatedAt(entity.getCreatedAt());
        milestone.setUpdatedAt(entity.getUpdatedAt());
        return milestone;
    }

    private GoalMilestoneEntity toEntity(GoalMilestone milestone) {
        GoalMilestoneEntity entity = new GoalMilestoneEntity();
        entity.setId(milestone.getId());
        entity.setGoalId(milestone.getGoalId());
        entity.setTitle(milestone.getTitle());
        entity.setTargetValue(milestone.getTargetValue());
        entity.setAchieved(milestone.getAchieved());
        entity.setAchievedDate(milestone.getAchievedDate());
        entity.setCreatedAt(milestone.getCreatedAt());
        entity.setUpdatedAt(milestone.getUpdatedAt());
        return entity;
    }
}