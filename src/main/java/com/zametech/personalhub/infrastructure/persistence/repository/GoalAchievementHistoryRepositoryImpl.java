package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.GoalAchievementHistory;
import com.zametech.personalhub.domain.repository.GoalAchievementHistoryRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.GoalAchievementHistoryEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaGoalAchievementHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GoalAchievementHistoryRepositoryImpl implements GoalAchievementHistoryRepository {
    private final JpaGoalAchievementHistoryRepository jpaRepository;

    @Override
    public GoalAchievementHistory save(GoalAchievementHistory achievement) {
        GoalAchievementHistoryEntity entity = toEntity(achievement);
        GoalAchievementHistoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<GoalAchievementHistory> findByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate) {
        return jpaRepository.findByGoalIdAndAchievedDate(goalId, achievedDate).map(this::toDomain);
    }

    @Override
    public List<GoalAchievementHistory> findByGoalId(Long goalId) {
        return jpaRepository.findByGoalId(goalId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<GoalAchievementHistory> findByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByGoalIdAndAchievedDateBetween(goalId, startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(GoalAchievementHistory achievement) {
        GoalAchievementHistoryEntity entity = toEntity(achievement);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByGoalIdAndAchievedDate(Long goalId, LocalDate achievedDate) {
        jpaRepository.deleteByGoalIdAndAchievedDate(goalId, achievedDate);
    }

    @Override
    public long countByGoalId(Long goalId) {
        return jpaRepository.countByGoalId(goalId);
    }

    @Override
    public long countByGoalIdAndAchievedDateBetween(Long goalId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.countByGoalIdAndAchievedDateBetween(goalId, startDate, endDate);
    }

    private GoalAchievementHistory toDomain(GoalAchievementHistoryEntity entity) {
        GoalAchievementHistory achievement = new GoalAchievementHistory();
        achievement.setId(entity.getId());
        achievement.setGoalId(entity.getGoalId());
        achievement.setAchievedDate(entity.getAchievedDate());
        achievement.setCreatedAt(entity.getCreatedAt());
        return achievement;
    }

    private GoalAchievementHistoryEntity toEntity(GoalAchievementHistory achievement) {
        GoalAchievementHistoryEntity entity = new GoalAchievementHistoryEntity();
        entity.setId(achievement.getId());
        entity.setGoalId(achievement.getGoalId());
        entity.setAchievedDate(achievement.getAchievedDate());
        entity.setCreatedAt(achievement.getCreatedAt());
        return entity;
    }
}