package com.zametech.personalhub.application.service;

import com.zametech.personalhub.application.goal.dto.GoalWithTrackingResponse;
import com.zametech.personalhub.application.goal.dto.ToggleAchievementResponse;
import com.zametech.personalhub.domain.model.Goal;
import com.zametech.personalhub.domain.model.GoalAchievementHistory;
import com.zametech.personalhub.domain.model.GoalType;
import com.zametech.personalhub.domain.repository.GoalAchievementHistoryRepository;
import com.zametech.personalhub.domain.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalAchievementHistoryRepository achievementHistoryRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private GoalService goalService;

    private UUID userId;
    private Goal goal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        goal = new Goal();
        goal.setId(1L);
        goal.setTitle("Test Goal");
        goal.setDescription("Test Description");
        goal.setGoalType(GoalType.DAILY);
        goal.setIsActive(true);
        goal.setUserId(userId);
        goal.setStartDate(LocalDate.now());
        goal.setEndDate(LocalDate.now().plusDays(30));
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createGoal_withValidGoal_shouldSaveAndReturnGoal() {
        // Given
        Goal newGoal = new Goal();
        newGoal.setTitle("New Goal");
        newGoal.setDescription("New Description");
        newGoal.setGoalType(GoalType.WEEKLY);
        newGoal.setStartDate(LocalDate.now());
        newGoal.setEndDate(LocalDate.now().plusWeeks(4));

        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal savedGoal = invocation.getArgument(0);
            savedGoal.setId(2L);
            return savedGoal;
        });

        // When
        Goal result = goalService.createGoal(newGoal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("New Goal");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void updateGoal_withValidGoalId_shouldUpdateAndReturnGoal() {
        // Given
        Goal updateData = new Goal();
        updateData.setTitle("Updated Title");
        updateData.setDescription("Updated Description");
        updateData.setIsActive(false);
        updateData.setEndDate(LocalDate.now().plusDays(60));

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Goal result = goalService.updateGoal(1L, updateData);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getIsActive()).isFalse();
        assertThat(result.getEndDate()).isEqualTo(updateData.getEndDate());
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(goalRepository).save(goal);
    }

    @Test
    void updateGoal_withNonExistentGoal_shouldThrowException() {
        // Given
        when(goalRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> goalService.updateGoal(999L, new Goal()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Goal not found or access denied");
    }

    @Test
    void updateGoal_withDifferentUserId_shouldThrowException() {
        // Given
        Goal otherUserGoal = new Goal();
        otherUserGoal.setId(2L);
        otherUserGoal.setUserId(UUID.randomUUID());
        
        when(goalRepository.findById(2L)).thenReturn(Optional.of(otherUserGoal));

        // When & Then
        assertThatThrownBy(() -> goalService.updateGoal(2L, new Goal()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Goal not found or access denied");
    }

    @Test
    void deleteGoal_withValidGoalId_shouldDeleteGoal() {
        // Given
        when(goalRepository.existsByIdAndUserId(1L, userId)).thenReturn(true);

        // When
        goalService.deleteGoal(1L);

        // Then
        verify(goalRepository).deleteById(1L);
    }

    @Test
    void deleteGoal_withNonExistentGoal_shouldThrowException() {
        // Given
        when(goalRepository.existsByIdAndUserId(999L, userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> goalService.deleteGoal(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Goal not found or access denied");
    }

    @Test
    void getGoalById_withValidGoalId_shouldReturnGoal() {
        // Given
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        // When
        Goal result = goalService.getGoalById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Goal");
    }

    @Test
    void getGoalById_withNonExistentGoal_shouldThrowException() {
        // Given
        when(goalRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> goalService.getGoalById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Goal not found or access denied");
    }

    @Test
    void getUserGoals_shouldReturnAllUserGoals() {
        // Given
        List<Goal> goals = Arrays.asList(goal, createGoal(2L, "Goal 2", GoalType.WEEKLY));
        when(goalRepository.findByUserId(userId)).thenReturn(goals);

        // When
        List<Goal> result = goalService.getUserGoals();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Goal::getTitle).containsExactly("Test Goal", "Goal 2");
    }

    @Test
    void getActiveGoals_shouldReturnOnlyActiveGoals() {
        // Given
        Goal activeGoal1 = createGoal(1L, "Active 1", GoalType.DAILY);
        Goal activeGoal2 = createGoal(2L, "Active 2", GoalType.WEEKLY);
        List<Goal> activeGoals = Arrays.asList(activeGoal1, activeGoal2);
        
        when(goalRepository.findByUserIdAndIsActive(userId, true)).thenReturn(activeGoals);

        // When
        List<Goal> result = goalService.getActiveGoals();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Goal::getIsActive);
    }

    @Test
    void getGoalsByType_shouldReturnGoalsOfSpecificType() {
        // Given
        Goal dailyGoal1 = createGoal(1L, "Daily 1", GoalType.DAILY);
        Goal dailyGoal2 = createGoal(2L, "Daily 2", GoalType.DAILY);
        List<Goal> dailyGoals = Arrays.asList(dailyGoal1, dailyGoal2);
        
        when(goalRepository.findByUserIdAndGoalType(userId, GoalType.DAILY)).thenReturn(dailyGoals);

        // When
        List<Goal> result = goalService.getGoalsByType(GoalType.DAILY);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(g -> g.getGoalType() == GoalType.DAILY);
    }

    @Test
    void getGoalWithTracking_shouldReturnGoalWithTrackingInfo() {
        // Given
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        // When
        GoalWithTrackingResponse result = goalService.getGoalWithTracking(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Test Goal");
        assertThat(result.trackingInfo()).isNotNull();
        assertThat(result.trackingInfo().todayStatus()).isEqualTo("not_started");
        assertThat(result.trackingInfo().achievementRate()).isEqualTo(0.0);
    }

    @Test
    void resetWeeklyGoals_shouldUpdateTimestampForWeeklyGoals() {
        // Given
        Goal weeklyGoal1 = createGoal(1L, "Weekly 1", GoalType.WEEKLY);
        Goal weeklyGoal2 = createGoal(2L, "Weekly 2", GoalType.WEEKLY);
        List<Goal> weeklyGoals = Arrays.asList(weeklyGoal1, weeklyGoal2);
        
        when(goalRepository.findByUserIdAndGoalType(userId, GoalType.WEEKLY)).thenReturn(weeklyGoals);
        when(goalRepository.save(any(Goal.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        goalService.resetWeeklyGoals();

        // Then
        verify(goalRepository, times(2)).save(any(Goal.class));
        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository, times(2)).save(goalCaptor.capture());
        
        List<Goal> savedGoals = goalCaptor.getAllValues();
        assertThat(savedGoals).allMatch(g -> g.getUpdatedAt() != null);
    }

    @Test
    void toggleAchievement_whenNotAchieved_shouldCreateAchievement() {
        // Given
        LocalDate today = LocalDate.now();
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(achievementHistoryRepository.findByGoalIdAndAchievedDate(1L, today))
            .thenReturn(Optional.empty());
        
        GoalAchievementHistory savedAchievement = new GoalAchievementHistory();
        savedAchievement.setId(10L);
        savedAchievement.setGoalId(1L);
        savedAchievement.setAchievedDate(today);
        savedAchievement.setCreatedAt(LocalDateTime.now());
        
        when(achievementHistoryRepository.save(any(GoalAchievementHistory.class)))
            .thenReturn(savedAchievement);

        // When
        ToggleAchievementResponse result = goalService.toggleAchievement(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.goalId()).isEqualTo("1");
        assertThat(result.periodType()).isEqualTo("DAILY");
        assertThat(result.periodDate()).isEqualTo(today.toString());
        assertThat(result.achieved()).isTrue();
        assertThat(result.achievementId()).isEqualTo("10");
        
        verify(achievementHistoryRepository).save(any(GoalAchievementHistory.class));
    }

    @Test
    void toggleAchievement_whenAlreadyAchieved_shouldDeleteAchievement() {
        // Given
        LocalDate today = LocalDate.now();
        GoalAchievementHistory existingAchievement = new GoalAchievementHistory();
        existingAchievement.setId(10L);
        existingAchievement.setGoalId(1L);
        existingAchievement.setAchievedDate(today);
        
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(achievementHistoryRepository.findByGoalIdAndAchievedDate(1L, today))
            .thenReturn(Optional.of(existingAchievement));

        // When
        ToggleAchievementResponse result = goalService.toggleAchievement(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.goalId()).isEqualTo("1");
        assertThat(result.achieved()).isFalse();
        assertThat(result.achievementId()).isNull();
        
        verify(achievementHistoryRepository).delete(existingAchievement);
        verify(achievementHistoryRepository, never()).save(any(GoalAchievementHistory.class));
    }

    private Goal createGoal(Long id, String title, GoalType type) {
        Goal g = new Goal();
        g.setId(id);
        g.setTitle(title);
        g.setGoalType(type);
        g.setIsActive(true);
        g.setUserId(userId);
        g.setStartDate(LocalDate.now());
        g.setEndDate(LocalDate.now().plusDays(30));
        g.setCreatedAt(LocalDateTime.now());
        g.setUpdatedAt(LocalDateTime.now());
        return g;
    }
}