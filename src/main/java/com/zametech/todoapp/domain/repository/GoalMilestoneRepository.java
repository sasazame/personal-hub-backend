package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.GoalMilestone;

import java.util.List;
import java.util.Optional;

public interface GoalMilestoneRepository {
    GoalMilestone save(GoalMilestone milestone);
    Optional<GoalMilestone> findById(Long id);
    List<GoalMilestone> findByGoalId(Long goalId);
    List<GoalMilestone> findByGoalIdAndAchieved(Long goalId, Boolean achieved);
    void deleteById(Long id);
    void deleteByGoalId(Long goalId);
}