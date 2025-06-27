package com.zametech.todoapp.infrastructure.goal.persistence.repository;

import com.zametech.todoapp.domain.goal.model.GoalStreak;
import com.zametech.todoapp.domain.goal.repository.GoalStreakRepository;
import com.zametech.todoapp.infrastructure.goal.persistence.entity.GoalStreakEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class GoalStreakRepositoryImpl implements GoalStreakRepository {
    
    private final JpaGoalStreakRepository jpaRepository;

    public GoalStreakRepositoryImpl(JpaGoalStreakRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<GoalStreak> findByGoalId(Long goalId) {
        return jpaRepository.findByGoalId(goalId)
                .map(this::toDomain);
    }

    @Override
    public GoalStreak save(GoalStreak goalStreak) {
        GoalStreakEntity entity = toEntity(goalStreak);
        if (goalStreak.getId() != null) {
            entity.setId(goalStreak.getId());
        }
        GoalStreakEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteByGoalId(Long goalId) {
        jpaRepository.deleteByGoalId(goalId);
    }

    private GoalStreak toDomain(GoalStreakEntity entity) {
        GoalStreak domain = new GoalStreak();
        domain.setId(entity.getId());
        domain.setGoalId(entity.getGoalId());
        domain.setCurrentStreak(entity.getCurrentStreak());
        domain.setLongestStreak(entity.getLongestStreak());
        domain.setLastAchievedDate(entity.getLastAchievedDate());
        domain.setStreakBrokenDate(entity.getStreakBrokenDate());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    private GoalStreakEntity toEntity(GoalStreak domain) {
        GoalStreakEntity entity = new GoalStreakEntity();
        entity.setGoalId(domain.getGoalId());
        entity.setCurrentStreak(domain.getCurrentStreak());
        entity.setLongestStreak(domain.getLongestStreak());
        entity.setLastAchievedDate(domain.getLastAchievedDate());
        entity.setStreakBrokenDate(domain.getStreakBrokenDate());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        return entity;
    }
}