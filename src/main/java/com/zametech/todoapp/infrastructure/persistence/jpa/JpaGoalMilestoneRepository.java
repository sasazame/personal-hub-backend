package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.infrastructure.persistence.entity.GoalMilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaGoalMilestoneRepository extends JpaRepository<GoalMilestoneEntity, Long> {
    List<GoalMilestoneEntity> findByGoalId(Long goalId);
    List<GoalMilestoneEntity> findByGoalIdAndAchieved(Long goalId, Boolean achieved);
    void deleteByGoalId(Long goalId);
}