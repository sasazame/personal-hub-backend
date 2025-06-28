package com.zametech.todoapp.application.goal.service;

import com.zametech.todoapp.application.goal.dto.*;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.model.Goal;
import com.zametech.todoapp.domain.model.GoalAchievementHistory;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.GoalAchievementHistoryRepository;
import com.zametech.todoapp.domain.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GoalServiceV2Test {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalAchievementHistoryRepository achievementRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private GoalServiceV2 goalService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setWeekStartDay(1); // Monday

        when(userContextService.getCurrentUserId()).thenReturn(userId);
        lenient().when(userContextService.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void getGoalsByDateAndFilter_ShouldReturnActiveGoals() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 1, 15);
        List<Goal> mockGoals = createMockGoals();
        when(goalRepository.findByUserId(userId)).thenReturn(mockGoals);

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "active");

        // Then
        assertNotNull(response);
        assertEquals(1, response.daily().size());
        assertEquals(1, response.weekly().size());
        assertEquals(0, response.monthly().size()); // Monthly goal is inactive
        assertEquals(0, response.annual().size());
    }

    @Test
    void getGoalsByDateAndFilter_ShouldReturnAllGoals() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 1, 15);
        List<Goal> mockGoals = createMockGoals();
        when(goalRepository.findByUserId(userId)).thenReturn(mockGoals);

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "all");

        // Then
        assertNotNull(response);
        assertEquals(1, response.daily().size());
        assertEquals(1, response.weekly().size());
        assertEquals(1, response.monthly().size());
        assertEquals(0, response.annual().size());
    }

    @Test
    void createGoal_ShouldCreateGoalWithDefaultDates() {
        // Given
        CreateGoalRequest request = new CreateGoalRequest(
                "New Goal",
                "Description",
                GoalType.DAILY,
                null,
                null
        );

        Goal savedGoal = new Goal();
        savedGoal.setId(1L);
        savedGoal.setTitle(request.title());
        savedGoal.setGoalType(request.goalType());
        savedGoal.setStartDate(LocalDate.of(2025, 1, 1));
        savedGoal.setEndDate(LocalDate.of(2025, 12, 31));

        when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);

        // When
        Goal result = goalService.createGoal(request);

        // Then
        assertNotNull(result);
        assertEquals("New Goal", result.getTitle());
        assertEquals(GoalType.DAILY, result.getGoalType());
        assertEquals(LocalDate.of(2025, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2025, 12, 31), result.getEndDate());
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void updateGoal_ShouldUpdateExistingGoal() {
        // Given
        Long goalId = 1L;
        Goal existingGoal = createTestGoal(goalId, GoalType.DAILY, true);
        UpdateGoalRequest request = new UpdateGoalRequest(
                "Updated Title",
                "Updated Description",
                false
        );

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existingGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(existingGoal);

        // When
        Goal result = goalService.updateGoal(goalId, request);

        // Then
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertFalse(result.getIsActive());
        verify(goalRepository).save(existingGoal);
    }

    @Test
    void deleteGoal_ShouldDeleteGoal() {
        // Given
        Long goalId = 1L;
        when(goalRepository.existsByIdAndUserId(goalId, userId)).thenReturn(true);

        // When
        goalService.deleteGoal(goalId);

        // Then
        verify(goalRepository).deleteById(goalId);
    }

    @Test
    void toggleAchievement_ShouldAddAchievement() {
        // Given
        Long goalId = 1L;
        LocalDate achievementDate = LocalDate.now();
        Goal goal = createTestGoal(goalId, GoalType.DAILY, true);

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(achievementRepository.findByGoalIdAndAchievedDate(goalId, achievementDate))
                .thenReturn(Optional.empty());

        // When
        goalService.toggleAchievement(goalId, achievementDate);

        // Then
        verify(achievementRepository).save(any(GoalAchievementHistory.class));
    }

    @Test
    void toggleAchievement_ShouldRemoveExistingAchievement() {
        // Given
        Long goalId = 1L;
        LocalDate achievementDate = LocalDate.now();
        Goal goal = createTestGoal(goalId, GoalType.DAILY, true);
        GoalAchievementHistory existingAchievement = new GoalAchievementHistory();
        existingAchievement.setId(1L);
        existingAchievement.setGoalId(goalId);
        existingAchievement.setAchievedDate(achievementDate);

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(achievementRepository.findByGoalIdAndAchievedDate(goalId, achievementDate))
                .thenReturn(Optional.of(existingAchievement));

        // When
        goalService.toggleAchievement(goalId, achievementDate);

        // Then
        verify(achievementRepository).delete(existingAchievement);
    }

    @Test
    void getAchievementHistory_ShouldReturnCorrectHistory() {
        // Given
        Long goalId = 1L;
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 5);
        Goal goal = createTestGoal(goalId, GoalType.DAILY, true);
        
        List<GoalAchievementHistory> achievements = Arrays.asList(
                createAchievement(goalId, LocalDate.of(2025, 1, 1)),
                createAchievement(goalId, LocalDate.of(2025, 1, 3)),
                createAchievement(goalId, LocalDate.of(2025, 1, 5))
        );

        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
        when(achievementRepository.findByGoalIdAndAchievedDateBetween(goalId, startDate, endDate))
                .thenReturn(achievements);

        // When
        AchievementHistoryResponse response = goalService.getAchievementHistory(goalId, startDate, endDate);

        // Then
        assertNotNull(response);
        assertEquals(5, response.totalDays());
        assertEquals(3, response.achievedDays());
        assertEquals(0.6, response.achievementRate());
        assertEquals(5, response.achievements().size());
    }

    @Test
    void getGoalsByDateAndFilter_WeeklyGoal_ShouldShowCompletedIfAchievedAnyDayInWeek() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 1, 15); // Wednesday
        LocalDate weekStart = LocalDate.of(2025, 1, 13); // Monday
        Long goalId = 1L;
        
        Goal weeklyGoal = createTestGoal(goalId, GoalType.WEEKLY, true);
        when(goalRepository.findByUserId(userId)).thenReturn(Arrays.asList(weeklyGoal));
        
        // Achievement on Tuesday (not the week start)
        GoalAchievementHistory achievement = createAchievement(goalId, LocalDate.of(2025, 1, 14));
        when(achievementRepository.findByGoalIdAndAchievedDateBetween(
                goalId, weekStart, weekStart.plusDays(6)))
                .thenReturn(Arrays.asList(achievement));
        when(achievementRepository.findByGoalId(goalId)).thenReturn(Arrays.asList(achievement));

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "active");

        // Then
        assertEquals(1, response.weekly().size());
        GoalResponse goalResponse = response.weekly().get(0);
        assertTrue(goalResponse.completed(), "Weekly goal should be marked as completed");
    }

    @Test
    void getGoalsByDateAndFilter_MonthlyGoal_ShouldShowCompletedIfAchievedAnyDayInMonth() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 1, 20); // January 20th
        Long goalId = 1L;
        
        Goal monthlyGoal = createTestGoal(goalId, GoalType.MONTHLY, true);
        when(goalRepository.findByUserId(userId)).thenReturn(Arrays.asList(monthlyGoal));
        
        // Achievement on January 5th (not the first day of month)
        GoalAchievementHistory achievement = createAchievement(goalId, LocalDate.of(2025, 1, 5));
        when(achievementRepository.findByGoalIdAndAchievedDateBetween(
                goalId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .thenReturn(Arrays.asList(achievement));
        when(achievementRepository.findByGoalId(goalId)).thenReturn(Arrays.asList(achievement));

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "active");

        // Then
        assertEquals(1, response.monthly().size());
        GoalResponse goalResponse = response.monthly().get(0);
        assertTrue(goalResponse.completed(), "Monthly goal should be marked as completed");
    }

    @Test
    void getGoalsByDateAndFilter_AnnualGoal_ShouldShowCompletedIfAchievedAnyDayInYear() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 6, 15); // June 15th
        Long goalId = 1L;
        
        Goal annualGoal = createTestGoal(goalId, GoalType.ANNUAL, true);
        when(goalRepository.findByUserId(userId)).thenReturn(Arrays.asList(annualGoal));
        
        // Achievement on March 10th (not the first day of year)
        GoalAchievementHistory achievement = createAchievement(goalId, LocalDate.of(2025, 3, 10));
        when(achievementRepository.findByGoalIdAndAchievedDateBetween(
                goalId, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
                .thenReturn(Arrays.asList(achievement));
        when(achievementRepository.findByGoalId(goalId)).thenReturn(Arrays.asList(achievement));

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "active");

        // Then
        assertEquals(1, response.annual().size());
        GoalResponse goalResponse = response.annual().get(0);
        assertTrue(goalResponse.completed(), "Annual goal should be marked as completed");
    }

    @Test
    void getGoalsByDateAndFilter_DailyGoal_ShouldOnlyShowCompletedForSpecificDay() {
        // Given
        LocalDate testDate = LocalDate.of(2025, 1, 15);
        Long goalId = 1L;
        
        Goal dailyGoal = createTestGoal(goalId, GoalType.DAILY, true);
        when(goalRepository.findByUserId(userId)).thenReturn(Arrays.asList(dailyGoal));
        
        // Achievement on different day
        GoalAchievementHistory achievement = createAchievement(goalId, LocalDate.of(2025, 1, 14));
        when(achievementRepository.findByGoalIdAndAchievedDate(goalId, testDate))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByGoalId(goalId)).thenReturn(Arrays.asList(achievement));

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(testDate, "active");

        // Then
        assertEquals(1, response.daily().size());
        GoalResponse goalResponse = response.daily().get(0);
        assertFalse(goalResponse.completed(), "Daily goal should not be marked as completed for different day");
    }

    @Test
    void calculateStreak_ShouldReturnCorrectStreaks() {
        // Given
        Long goalId = 1L;
        LocalDate today = LocalDate.now();
        List<Goal> goals = Arrays.asList(createTestGoal(goalId, GoalType.DAILY, true));
        
        // Create achievements for current streak of 3 days
        List<GoalAchievementHistory> achievements = Arrays.asList(
                createAchievement(goalId, today),
                createAchievement(goalId, today.minusDays(1)),
                createAchievement(goalId, today.minusDays(2)),
                // Gap
                createAchievement(goalId, today.minusDays(5)),
                createAchievement(goalId, today.minusDays(6)),
                createAchievement(goalId, today.minusDays(7)),
                createAchievement(goalId, today.minusDays(8)),
                createAchievement(goalId, today.minusDays(9))
        );

        when(goalRepository.findByUserId(userId)).thenReturn(goals);
        when(achievementRepository.findByGoalId(goalId)).thenReturn(achievements);

        // When
        GroupedGoalsResponse response = goalService.getGoalsByDateAndFilter(today, "active");

        // Then
        assertEquals(1, response.daily().size());
        GoalResponse goalResponse = response.daily().get(0);
        assertEquals(3, goalResponse.currentStreak());
        assertEquals(5, goalResponse.longestStreak());
    }

    private List<Goal> createMockGoals() {
        List<Goal> goals = new ArrayList<>();
        
        // Active daily goal
        goals.add(createTestGoal(1L, GoalType.DAILY, true));
        
        // Active weekly goal
        goals.add(createTestGoal(2L, GoalType.WEEKLY, true));
        
        // Inactive monthly goal
        Goal monthlyGoal = createTestGoal(3L, GoalType.MONTHLY, false);
        goals.add(monthlyGoal);
        
        return goals;
    }

    private Goal createTestGoal(Long id, GoalType type, boolean isActive) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setUserId(userId);
        goal.setTitle(type + " Goal");
        goal.setDescription("Test " + type + " goal");
        goal.setGoalType(type);
        goal.setIsActive(isActive);
        goal.setStartDate(LocalDate.of(2025, 1, 1));
        goal.setEndDate(LocalDate.of(2025, 12, 31));
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        return goal;
    }

    private GoalAchievementHistory createAchievement(Long goalId, LocalDate date) {
        GoalAchievementHistory achievement = new GoalAchievementHistory();
        achievement.setId(Long.valueOf(UUID.randomUUID().hashCode()));
        achievement.setGoalId(goalId);
        achievement.setAchievedDate(date);
        achievement.setCreatedAt(LocalDateTime.now());
        return achievement;
    }
}