package com.zametech.todoapp.application.goal.service;

import com.zametech.todoapp.application.goal.dto.*;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalAchievementHistory;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.GoalAchievementHistoryRepository;
import com.zametech.todoapp.domain.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceV2 {
    private final GoalRepository goalRepository;
    private final GoalAchievementHistoryRepository achievementRepository;
    private final UserContextService userContextService;

    public GroupedGoalsResponse getGoalsByDateAndFilter(LocalDate date, String filter) {
        UUID userId = userContextService.getCurrentUserId();
        LocalDate queryDate = date != null ? date : LocalDate.now();
        
        List<Goal> allGoals = goalRepository.findByUserId(userId);
        
        // Apply filter
        List<Goal> filteredGoals = filterGoals(allGoals, queryDate, filter);
        
        // Group by type and calculate streaks
        Map<GoalType, List<GoalResponse>> groupedGoals = filteredGoals.stream()
                .map(goal -> toGoalResponse(goal, queryDate))
                .collect(Collectors.groupingBy(GoalResponse::goalType));
        
        return new GroupedGoalsResponse(
                groupedGoals.getOrDefault(GoalType.DAILY, new ArrayList<>()),
                groupedGoals.getOrDefault(GoalType.WEEKLY, new ArrayList<>()),
                groupedGoals.getOrDefault(GoalType.MONTHLY, new ArrayList<>()),
                groupedGoals.getOrDefault(GoalType.ANNUAL, new ArrayList<>())
        );
    }

    public Goal createGoal(CreateGoalRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        LocalDate now = LocalDate.now();
        
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setGoalType(request.goalType());
        goal.setIsActive(true);
        
        // Set default dates if not provided
        if (request.startDate() != null) {
            goal.setStartDate(request.startDate());
        } else {
            goal.setStartDate(LocalDate.of(now.getYear(), 1, 1));
        }
        
        if (request.endDate() != null) {
            goal.setEndDate(request.endDate());
        } else {
            goal.setEndDate(LocalDate.of(now.getYear(), 12, 31));
        }
        
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        
        return goalRepository.save(goal);
    }

    public Goal updateGoal(Long goalId, UpdateGoalRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or access denied"));
        
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        if (request.isActive() != null) {
            goal.setIsActive(request.isActive());
        }
        goal.setUpdatedAt(LocalDateTime.now());
        
        return goalRepository.save(goal);
    }

    public void deleteGoal(Long goalId) {
        UUID userId = userContextService.getCurrentUserId();
        if (!goalRepository.existsByIdAndUserId(goalId, userId)) {
            throw new RuntimeException("Goal not found or access denied");
        }
        
        // Achievement history will be deleted by cascade
        goalRepository.deleteById(goalId);
    }

    public void toggleAchievement(Long goalId, LocalDate date) {
        UUID userId = userContextService.getCurrentUserId();
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or access denied"));
        
        LocalDate achievementDate = date != null ? date : LocalDate.now();
        
        // Check if achievement exists
        Optional<GoalAchievementHistory> existing = achievementRepository
                .findByGoalIdAndAchievedDate(goalId, achievementDate);
        
        if (existing.isPresent()) {
            // Remove achievement
            achievementRepository.delete(existing.get());
        } else {
            // Add achievement
            GoalAchievementHistory achievement = new GoalAchievementHistory();
            achievement.setGoalId(goalId);
            achievement.setAchievedDate(achievementDate);
            achievement.setCreatedAt(LocalDateTime.now());
            achievementRepository.save(achievement);
        }
    }

    public AchievementHistoryResponse getAchievementHistory(Long goalId, LocalDate from, LocalDate to) {
        UUID userId = userContextService.getCurrentUserId();
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found or access denied"));
        
        LocalDate startDate = from != null ? from : goal.getStartDate();
        LocalDate endDate = to != null ? to : LocalDate.now();
        
        List<GoalAchievementHistory> achievements = achievementRepository
                .findByGoalIdAndAchievedDateBetween(goalId, startDate, endDate);
        
        Set<LocalDate> achievedDates = achievements.stream()
                .map(GoalAchievementHistory::getAchievedDate)
                .collect(Collectors.toSet());
        
        // Build achievement records for the date range
        List<AchievementHistoryResponse.AchievementRecord> records = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            records.add(new AchievementHistoryResponse.AchievementRecord(
                    current,
                    achievedDates.contains(current)
            ));
            current = current.plusDays(1);
        }
        
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int achievedDays = achievedDates.size();
        double achievementRate = totalDays > 0 ? (double) achievedDays / totalDays : 0.0;
        
        return new AchievementHistoryResponse(
                records,
                totalDays,
                achievedDays,
                achievementRate
        );
    }

    private List<Goal> filterGoals(List<Goal> goals, LocalDate date, String filter) {
        if (filter == null || filter.equals("all")) {
            return goals;
        }
        
        return goals.stream()
                .filter(goal -> {
                    boolean isDateInRange = !date.isBefore(goal.getStartDate()) && 
                                          !date.isAfter(goal.getEndDate());
                    
                    if (filter.equals("active")) {
                        return goal.getIsActive() && isDateInRange;
                    } else if (filter.equals("inactive")) {
                        return !goal.getIsActive() || !isDateInRange;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private GoalResponse toGoalResponse(Goal goal, LocalDate referenceDate) {
        // Calculate completion status
        boolean completed = isGoalCompletedForPeriod(goal, referenceDate);
        
        // Calculate streaks
        StreakInfo streakInfo = calculateStreak(goal, referenceDate);
        
        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getGoalType(),
                goal.getIsActive(),
                goal.getStartDate(),
                goal.getEndDate(),
                completed,
                streakInfo.currentStreak(),
                streakInfo.longestStreak(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    private boolean isGoalCompletedForPeriod(Goal goal, LocalDate referenceDate) {
        LocalDate periodDate = calculatePeriodDate(goal.getGoalType(), referenceDate);
        return achievementRepository.findByGoalIdAndAchievedDate(goal.getId(), periodDate).isPresent();
    }

    private LocalDate calculatePeriodDate(GoalType goalType, LocalDate referenceDate) {
        switch (goalType) {
            case DAILY:
                return referenceDate;
            case WEEKLY:
                User user = userContextService.getCurrentUser();
                Integer weekStartDay = user.getWeekStartDay() != null ? user.getWeekStartDay() : 1;
                return getWeekStart(referenceDate, weekStartDay);
            case MONTHLY:
                return referenceDate.with(TemporalAdjusters.firstDayOfMonth());
            case ANNUAL:
                return referenceDate.with(TemporalAdjusters.firstDayOfYear());
            default:
                return referenceDate;
        }
    }

    private StreakInfo calculateStreak(Goal goal, LocalDate referenceDate) {
        List<GoalAchievementHistory> achievements = achievementRepository.findByGoalId(goal.getId());
        
        if (achievements.isEmpty()) {
            return new StreakInfo(0, 0);
        }
        
        Set<LocalDate> achievedDates = achievements.stream()
                .map(history -> normalizeDate(history.getAchievedDate(), goal.getGoalType()))
                .collect(Collectors.toSet());
        
        // Calculate current streak
        int currentStreak = calculateCurrentStreak(achievedDates, goal.getGoalType(), referenceDate);
        
        // Calculate longest streak
        int longestStreak = calculateLongestStreak(achievedDates, goal.getGoalType());
        
        return new StreakInfo(currentStreak, longestStreak);
    }

    private LocalDate normalizeDate(LocalDate date, GoalType goalType) {
        switch (goalType) {
            case DAILY:
                return date;
            case WEEKLY:
                User user = userContextService.getCurrentUser();
                Integer weekStartDay = user.getWeekStartDay() != null ? user.getWeekStartDay() : 1;
                return getWeekStart(date, weekStartDay);
            case MONTHLY:
                return date.with(TemporalAdjusters.firstDayOfMonth());
            case ANNUAL:
                return date.with(TemporalAdjusters.firstDayOfYear());
            default:
                return date;
        }
    }

    private int calculateCurrentStreak(Set<LocalDate> achievedDates, GoalType goalType, LocalDate referenceDate) {
        LocalDate current = normalizeDate(referenceDate, goalType);
        int streak = 0;
        
        while (achievedDates.contains(current)) {
            streak++;
            current = getPreviousPeriod(current, goalType);
        }
        
        return streak;
    }

    private int calculateLongestStreak(Set<LocalDate> achievedDates, GoalType goalType) {
        if (achievedDates.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> sortedDates = new ArrayList<>(achievedDates);
        sortedDates.sort(Comparator.naturalOrder());
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate prevDate = sortedDates.get(i - 1);
            LocalDate currDate = sortedDates.get(i);
            
            if (isConsecutivePeriod(prevDate, currDate, goalType)) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }

    private boolean isConsecutivePeriod(LocalDate date1, LocalDate date2, GoalType goalType) {
        LocalDate nextPeriod = getNextPeriod(date1, goalType);
        return nextPeriod.equals(date2);
    }

    private LocalDate getPreviousPeriod(LocalDate date, GoalType goalType) {
        switch (goalType) {
            case DAILY:
                return date.minusDays(1);
            case WEEKLY:
                return date.minusWeeks(1);
            case MONTHLY:
                return date.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            case ANNUAL:
                return date.minusYears(1).with(TemporalAdjusters.firstDayOfYear());
            default:
                return date.minusDays(1);
        }
    }

    private LocalDate getNextPeriod(LocalDate date, GoalType goalType) {
        switch (goalType) {
            case DAILY:
                return date.plusDays(1);
            case WEEKLY:
                return date.plusWeeks(1);
            case MONTHLY:
                return date.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            case ANNUAL:
                return date.plusYears(1).with(TemporalAdjusters.firstDayOfYear());
            default:
                return date.plusDays(1);
        }
    }

    private LocalDate getWeekStart(LocalDate date, int weekStartDay) {
        DayOfWeek startDay = DayOfWeek.of(weekStartDay == 0 ? 7 : weekStartDay);
        return date.with(TemporalAdjusters.previousOrSame(startDay));
    }

    private record StreakInfo(int currentStreak, int longestStreak) {}
}