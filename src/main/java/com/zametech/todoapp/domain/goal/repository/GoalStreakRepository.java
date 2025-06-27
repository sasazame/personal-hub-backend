package com.zametech.todoapp.domain.goal.repository;

import com.zametech.todoapp.domain.goal.model.GoalStreak;
import java.util.Optional;

public interface GoalStreakRepository {
    Optional<GoalStreak> findByGoalId(Long goalId);
    GoalStreak save(GoalStreak goalStreak);
    void deleteByGoalId(Long goalId);
}