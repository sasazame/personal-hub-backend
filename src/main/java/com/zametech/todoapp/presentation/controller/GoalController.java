package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.goal.dto.GoalWithTrackingResponse;
import com.zametech.todoapp.application.goal.dto.ToggleAchievementResponse;
import com.zametech.todoapp.application.service.GoalService;
import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalMilestone;
import com.zametech.todoapp.domain.model.GoalProgress;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.repository.GoalMilestoneRepository;
import com.zametech.todoapp.presentation.dto.request.CreateGoalRequest;
import com.zametech.todoapp.presentation.dto.request.RecordProgressRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateGoalRequest;
import com.zametech.todoapp.presentation.dto.response.GoalMilestoneResponse;
import com.zametech.todoapp.presentation.dto.response.GoalProgressResponse;
import com.zametech.todoapp.presentation.dto.response.GoalResponse;
import com.zametech.todoapp.presentation.mapper.GoalMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goalService;
    private final GoalMilestoneRepository goalMilestoneRepository;
    private final GoalMapper goalMapper;

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        Goal goal = goalMapper.toGoal(request);
        Goal createdGoal = goalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(goalMapper.toGoalResponse(createdGoal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalWithTrackingResponse> getGoal(@PathVariable Long id) {
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
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        Goal goalUpdate = new Goal();
        goalUpdate.setTitle(request.getTitle());
        goalUpdate.setDescription(request.getDescription());
        goalUpdate.setTargetValue(request.getTargetValue());
        goalUpdate.setMetricUnit(request.getMetricUnit());
        goalUpdate.setEndDate(request.getEndDate());
        
        Goal updatedGoal = goalService.updateGoal(id, goalUpdate);
        return ResponseEntity.ok(goalMapper.toGoalResponse(updatedGoal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<GoalProgressResponse> recordProgress(
            @PathVariable Long id,
            @Valid @RequestBody RecordProgressRequest request) {
        GoalProgress progress = goalService.recordProgress(
                id, request.getValue(), request.getDate(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalMapper.toGoalProgressResponse(progress));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<List<GoalProgressResponse>> getGoalProgress(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<GoalProgress> progressList = goalService.getGoalProgress(id, startDate, endDate);
        List<GoalProgressResponse> responses = progressList.stream()
                .map(goalMapper::toGoalProgressResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/weekly-progress")
    public ResponseEntity<BigDecimal> getWeeklyProgress(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer weekStartDay) {
        BigDecimal weeklyProgress = goalService.calculateWeeklyProgress(id, weekStartDay);
        return ResponseEntity.ok(weeklyProgress);
    }

    @GetMapping("/{id}/milestones")
    public ResponseEntity<List<GoalMilestoneResponse>> getGoalMilestones(@PathVariable Long id) {
        goalService.getGoalById(id); // Verify ownership
        List<GoalMilestone> milestones = goalMilestoneRepository.findByGoalId(id);
        List<GoalMilestoneResponse> responses = milestones.stream()
                .map(goalMapper::toGoalMilestoneResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/reset-weekly")
    public ResponseEntity<Void> resetWeeklyGoals() {
        goalService.resetWeeklyGoals();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-achievement")
    public ResponseEntity<ToggleAchievementResponse> toggleAchievement(@PathVariable Long id) {
        ToggleAchievementResponse response = goalService.toggleAchievement(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/achievement-history")
    public ResponseEntity<List<GoalProgressResponse>> getAchievementHistory(@PathVariable Long id) {
        List<GoalProgress> achievements = goalService.getAchievementHistory(id);
        List<GoalProgressResponse> responses = achievements.stream()
                .map(goalMapper::toGoalProgressResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}