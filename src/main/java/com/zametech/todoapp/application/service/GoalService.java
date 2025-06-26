package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.*;
import com.zametech.todoapp.domain.repository.GoalMilestoneRepository;
import com.zametech.todoapp.domain.repository.GoalProgressRepository;
import com.zametech.todoapp.domain.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalService {
    private final GoalRepository goalRepository;
    private final GoalProgressRepository goalProgressRepository;
    private final GoalMilestoneRepository goalMilestoneRepository;
    private final UserContextService userContextService;

    public Goal createGoal(Goal goal) {
        UUID userId = userContextService.getCurrentUserId();
        goal.setUserId(userId);
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setCurrentValue(BigDecimal.ZERO);
        
        Goal savedGoal = goalRepository.save(goal);
        
        // Create default milestones based on goal type
        createDefaultMilestones(savedGoal);
        
        return savedGoal;
    }

    public Goal updateGoal(Long goalId, Goal goalUpdate) {
        UUID userId = userContextService.getCurrentUserId();
        Goal existingGoal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or access denied"));

        existingGoal.setTitle(goalUpdate.getTitle());
        existingGoal.setDescription(goalUpdate.getDescription());
        existingGoal.setTargetValue(goalUpdate.getTargetValue());
        existingGoal.setMetricUnit(goalUpdate.getMetricUnit());
        existingGoal.setEndDate(goalUpdate.getEndDate());

        return goalRepository.save(existingGoal);
    }

    public void deleteGoal(Long goalId) {
        UUID userId = userContextService.getCurrentUserId();
        if (!goalRepository.existsByIdAndUserId(goalId, userId)) {
            throw new RuntimeException("Goal not found or access denied");
        }
        
        // Delete related data
        goalProgressRepository.deleteByGoalId(goalId);
        goalMilestoneRepository.deleteByGoalId(goalId);
        goalRepository.deleteById(goalId);
    }

    public Goal getGoalById(Long goalId) {
        UUID userId = userContextService.getCurrentUserId();
        return goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or access denied"));
    }

    public List<Goal> getUserGoals() {
        UUID userId = userContextService.getCurrentUserId();
        return goalRepository.findByUserId(userId);
    }

    public List<Goal> getActiveGoals() {
        UUID userId = userContextService.getCurrentUserId();
        return goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
    }

    public List<Goal> getGoalsByType(GoalType goalType) {
        UUID userId = userContextService.getCurrentUserId();
        return goalRepository.findByUserIdAndGoalType(userId, goalType);
    }

    public GoalProgress recordProgress(Long goalId, BigDecimal value, LocalDate date, String note) {
        UUID userId = userContextService.getCurrentUserId();
        Goal goal = getGoalById(goalId);
        
        // Check if progress already exists for this date
        GoalProgress progress = goalProgressRepository.findByGoalIdAndDate(goalId, date)
                .orElse(new GoalProgress());
        
        progress.setGoalId(goalId);
        progress.setDate(date);
        progress.setValue(value);
        progress.setNote(note);
        
        GoalProgress savedProgress = goalProgressRepository.save(progress);
        
        // Update goal's current value
        updateGoalCurrentValue(goal);
        
        // Check and update milestones
        checkAndUpdateMilestones(goal);
        
        return savedProgress;
    }

    public List<GoalProgress> getGoalProgress(Long goalId, LocalDate startDate, LocalDate endDate) {
        UUID userId = userContextService.getCurrentUserId();
        getGoalById(goalId); // Verify ownership
        return goalProgressRepository.findByGoalIdAndDateRange(goalId, startDate, endDate);
    }

    public BigDecimal calculateWeeklyProgress(Long goalId, int weekStartDay) {
        Goal goal = getGoalById(goalId);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = getWeekStart(today, weekStartDay);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        List<GoalProgress> weekProgress = goalProgressRepository.findByGoalIdAndDateRange(
                goalId, weekStart, weekEnd);
        
        return weekProgress.stream()
                .map(GoalProgress::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateGoalCurrentValue(Goal goal) {
        LocalDate startDate = goal.getStartDate();
        LocalDate endDate = LocalDate.now();
        
        if (goal.getGoalType() == GoalType.WEEKLY) {
            // For weekly goals, only consider current week
            User user = userContextService.getCurrentUser();
            Integer weekStartDay = user.getWeekStartDay() != null ? user.getWeekStartDay() : 1;
            startDate = getWeekStart(endDate, weekStartDay);
        } else if (goal.getGoalType() == GoalType.MONTHLY) {
            // For monthly goals, only consider current month
            startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
        }
        
        List<GoalProgress> progressList = goalProgressRepository.findByGoalIdAndDateRange(
                goal.getId(), startDate, endDate);
        
        BigDecimal currentValue = progressList.stream()
                .map(GoalProgress::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        goal.setCurrentValue(currentValue);
        goalRepository.save(goal);
    }

    private void checkAndUpdateMilestones(Goal goal) {
        List<GoalMilestone> milestones = goalMilestoneRepository.findByGoalIdAndAchieved(
                goal.getId(), false);
        
        for (GoalMilestone milestone : milestones) {
            if (goal.getCurrentValue().compareTo(milestone.getTargetValue()) >= 0) {
                milestone.setAchieved(true);
                milestone.setAchievedDate(LocalDate.now());
                goalMilestoneRepository.save(milestone);
            }
        }
        
        // Check if goal is completed
        if (goal.getCurrentValue().compareTo(goal.getTargetValue()) >= 0 
                && goal.getStatus() == GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.COMPLETED);
            goalRepository.save(goal);
        }
    }

    private void createDefaultMilestones(Goal goal) {
        if (goal.getGoalType() == GoalType.ANNUAL || goal.getGoalType() == GoalType.MONTHLY) {
            // Create 25%, 50%, 75% milestones
            BigDecimal[] percentages = {new BigDecimal("0.25"), new BigDecimal("0.50"), new BigDecimal("0.75")};
            String[] titles = {"25% Complete", "50% Complete", "75% Complete"};
            
            for (int i = 0; i < percentages.length; i++) {
                GoalMilestone milestone = new GoalMilestone();
                milestone.setGoalId(goal.getId());
                milestone.setTitle(titles[i]);
                milestone.setTargetValue(goal.getTargetValue().multiply(percentages[i]));
                milestone.setAchieved(false);
                goalMilestoneRepository.save(milestone);
            }
        }
    }

    private LocalDate getWeekStart(LocalDate date, int weekStartDay) {
        DayOfWeek startDay = DayOfWeek.of(weekStartDay == 0 ? 7 : weekStartDay);
        return date.with(TemporalAdjusters.previousOrSame(startDay));
    }

    public void resetWeeklyGoals() {
        UUID userId = userContextService.getCurrentUserId();
        List<Goal> weeklyGoals = goalRepository.findByUserIdAndGoalType(userId, GoalType.WEEKLY);
        
        for (Goal goal : weeklyGoals) {
            goal.setCurrentValue(BigDecimal.ZERO);
            goalRepository.save(goal);
        }
    }
}