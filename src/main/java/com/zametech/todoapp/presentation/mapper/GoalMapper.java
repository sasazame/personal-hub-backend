package com.zametech.todoapp.presentation.mapper;

import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalMilestone;
import com.zametech.todoapp.domain.model.GoalProgress;
import com.zametech.todoapp.presentation.dto.request.CreateGoalRequest;
import com.zametech.todoapp.presentation.dto.response.GoalMilestoneResponse;
import com.zametech.todoapp.presentation.dto.response.GoalProgressResponse;
import com.zametech.todoapp.presentation.dto.response.GoalResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GoalMapper {
    
    public Goal toGoal(CreateGoalRequest request) {
        Goal goal = new Goal();
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setGoalType(request.getGoalType());
        goal.setMetricType(request.getMetricType());
        goal.setMetricUnit(request.getMetricUnit());
        goal.setTargetValue(request.getTargetValue());
        goal.setStartDate(request.getStartDate());
        goal.setEndDate(request.getEndDate());
        return goal;
    }
    
    public GoalResponse toGoalResponse(Goal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setTitle(goal.getTitle());
        response.setDescription(goal.getDescription());
        response.setGoalType(goal.getGoalType());
        response.setMetricType(goal.getMetricType());
        response.setMetricUnit(goal.getMetricUnit());
        response.setTargetValue(goal.getTargetValue());
        response.setCurrentValue(goal.getCurrentValue());
        response.setProgressPercentage(calculateProgressPercentage(goal));
        response.setStartDate(goal.getStartDate());
        response.setEndDate(goal.getEndDate());
        response.setStatus(goal.getStatus());
        response.setCreatedAt(goal.getCreatedAt());
        response.setUpdatedAt(goal.getUpdatedAt());
        return response;
    }
    
    public GoalProgressResponse toGoalProgressResponse(GoalProgress progress) {
        GoalProgressResponse response = new GoalProgressResponse();
        response.setId(progress.getId());
        response.setGoalId(progress.getGoalId());
        response.setDate(progress.getDate());
        response.setValue(progress.getValue());
        response.setNote(progress.getNote());
        response.setCreatedAt(progress.getCreatedAt());
        response.setUpdatedAt(progress.getUpdatedAt());
        return response;
    }
    
    public GoalMilestoneResponse toGoalMilestoneResponse(GoalMilestone milestone) {
        GoalMilestoneResponse response = new GoalMilestoneResponse();
        response.setId(milestone.getId());
        response.setGoalId(milestone.getGoalId());
        response.setTitle(milestone.getTitle());
        response.setTargetValue(milestone.getTargetValue());
        response.setAchieved(milestone.getAchieved());
        response.setAchievedDate(milestone.getAchievedDate());
        response.setCreatedAt(milestone.getCreatedAt());
        response.setUpdatedAt(milestone.getUpdatedAt());
        return response;
    }
    
    private BigDecimal calculateProgressPercentage(Goal goal) {
        if (goal.getTargetValue() == null || goal.getTargetValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal currentValue = goal.getCurrentValue() != null ? goal.getCurrentValue() : BigDecimal.ZERO;
        return currentValue.multiply(new BigDecimal("100"))
                .divide(goal.getTargetValue(), 2, RoundingMode.HALF_UP);
    }
}