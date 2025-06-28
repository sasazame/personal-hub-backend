package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.goal.dto.*;
import com.zametech.todoapp.application.goal.service.GoalServiceV2;
import com.zametech.todoapp.domain.model.Goal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalServiceV2 goalService;

    @GetMapping
    public ResponseEntity<GroupedGoalsResponse> getGoals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "active") String filter) {
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(date, filter);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        Goal goal = goalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGoalRequest request) {
        Goal goal = goalService.updateGoal(id, request);
        return ResponseEntity.ok(goal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/achievements")
    public ResponseEntity<Void> toggleAchievement(
            @PathVariable Long id,
            @RequestBody(required = false) ToggleAchievementRequest request) {
        LocalDate date = request != null && request.date() != null ? request.date() : LocalDate.now();
        goalService.toggleAchievement(id, date);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/achievements")
    public ResponseEntity<Void> deleteAchievement(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        goalService.toggleAchievement(id, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/achievements")
    public ResponseEntity<AchievementHistoryResponse> getAchievementHistory(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AchievementHistoryResponse response = goalService.getAchievementHistory(id, from, to);
        return ResponseEntity.ok(response);
    }
}