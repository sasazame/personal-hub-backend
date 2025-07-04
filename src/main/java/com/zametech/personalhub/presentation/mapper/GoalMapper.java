package com.zametech.personalhub.presentation.mapper;

import com.zametech.personalhub.domain.model.Goal;
import com.zametech.personalhub.presentation.dto.request.CreateGoalRequest;
import com.zametech.personalhub.presentation.dto.response.GoalResponse;
import org.springframework.stereotype.Component;

@Component
public class GoalMapper {
    
    public Goal toGoal(CreateGoalRequest request) {
        Goal goal = new Goal();
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setGoalType(request.getGoalType());
        goal.setIsActive(true);
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
        response.setIsActive(goal.getIsActive());
        response.setStartDate(goal.getStartDate());
        response.setEndDate(goal.getEndDate());
        response.setCreatedAt(goal.getCreatedAt());
        response.setUpdatedAt(goal.getUpdatedAt());
        return response;
    }
}