package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.goal.dto.GoalWithTrackingResponse;
import com.zametech.todoapp.application.goal.dto.ToggleAchievementResponse;
import com.zametech.todoapp.application.service.GoalService;
import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.presentation.dto.request.CreateGoalRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateGoalRequest;
import com.zametech.todoapp.presentation.dto.response.GoalResponse;
import com.zametech.todoapp.presentation.mapper.GoalMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goalService;
    private final GoalMapper goalMapper;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        Goal goal = goalMapper.toGoal(request);
        Goal createdGoal = goalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(goalMapper.toGoalResponse(createdGoal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalWithTrackingResponse> getGoal(@PathVariable String id) {
        GoalWithTrackingResponse goalWithTracking = goalService.getGoalWithTracking(id);
        return ResponseEntity.ok(goalWithTracking);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals() {
        List<Goal> goals = goalService.getUserGoals();
        List<GoalResponse> responses = goals.stream()
                .map(goalMapper::toGoalResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<GoalResponse>> getActiveGoals() {
        List<Goal> goals = goalService.getActiveGoals();
        List<GoalResponse> responses = goals.stream()
                .map(goalMapper::toGoalResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/type/{goalType}")
    public ResponseEntity<List<GoalResponse>> getGoalsByType(@PathVariable GoalType goalType) {
        List<Goal> goals = goalService.getGoalsByType(goalType);
        List<GoalResponse> responses = goals.stream()
                .map(goalMapper::toGoalResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable String id,
            @Valid @RequestBody UpdateGoalRequest request) {
        Goal goalUpdate = new Goal();
        goalUpdate.setTitle(request.getTitle());
        goalUpdate.setDescription(request.getDescription());
        goalUpdate.setIsActive(request.getIsActive());
        goalUpdate.setEndDate(request.getEndDate());
        
        Goal updatedGoal = goalService.updateGoal(id, goalUpdate);
        return ResponseEntity.ok(goalMapper.toGoalResponse(updatedGoal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-weekly")
    public ResponseEntity<Void> resetWeeklyGoals() {
        goalService.resetWeeklyGoals();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-achievement")
    public ResponseEntity<ToggleAchievementResponse> toggleAchievement(@PathVariable String id) {
        ToggleAchievementResponse response = goalService.toggleAchievement(id);
        return ResponseEntity.ok(response);
    }
}