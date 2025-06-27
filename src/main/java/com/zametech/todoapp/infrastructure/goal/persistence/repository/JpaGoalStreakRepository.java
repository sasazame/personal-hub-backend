package com.zametech.todoapp.infrastructure.goal.persistence.repository;

import com.zametech.todoapp.infrastructure.goal.persistence.entity.GoalStreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface JpaGoalStreakRepository extends JpaRepository<GoalStreakEntity, Long> {
    Optional<GoalStreakEntity> findByGoalId(Long goalId);
    void deleteByGoalId(Long goalId);
}