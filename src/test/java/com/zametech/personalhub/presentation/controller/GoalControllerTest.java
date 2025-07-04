package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.goal.dto.*;
import com.zametech.personalhub.application.goal.service.GoalServiceV2;
import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Goal;
import com.zametech.personalhub.domain.model.GoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = GoalController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalServiceV2 goalService;

    private Goal sampleGoal;
    private CreateGoalRequest createGoalRequest;
    private UpdateGoalRequest updateGoalRequest;
    private GroupedGoalsResponse groupedGoalsResponse;
    private AchievementHistoryResponse achievementHistoryResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        sampleGoal = new Goal();
        sampleGoal.setId(1L);
        sampleGoal.setUserId(userId);
        sampleGoal.setTitle("Exercise Daily");
        sampleGoal.setDescription("30 minutes of exercise every day");
        sampleGoal.setGoalType(GoalType.DAILY);
        sampleGoal.setIsActive(true);
        sampleGoal.setStartDate(LocalDate.now().minusDays(7));
        sampleGoal.setEndDate(null);
        sampleGoal.setCreatedAt(now);
        sampleGoal.setUpdatedAt(now);

        createGoalRequest = new CreateGoalRequest(
            "Read Books",
            "Read at least 20 pages daily",
            GoalType.DAILY,
            LocalDate.now(),
            null
        );

        updateGoalRequest = new UpdateGoalRequest(
            "Exercise Daily - Updated",
            "45 minutes of exercise every day",
            true // isActive
        );

        // Create sample grouped goals response
        GoalTrackingInfo trackingInfo = new GoalTrackingInfo(
            30, // totalDays
            21, // achievedDays
            0.7, // achievementRate
            7, // currentStreak
            14, // longestStreak
            "ACHIEVED", // todayStatus
            "IN_PROGRESS", // currentPeriodStatus
            false // currentPeriodAchieved
        );
        
        GoalResponse goalResponse = new GoalResponse(
            sampleGoal.getId(),
            sampleGoal.getTitle(),
            sampleGoal.getDescription(),
            sampleGoal.getGoalType(),
            sampleGoal.getIsActive(),
            sampleGoal.getStartDate(),
            sampleGoal.getEndDate(),
            false, // completed
            7, // currentStreak
            14, // longestStreak
            sampleGoal.getCreatedAt(),
            sampleGoal.getUpdatedAt()
        );
        
        groupedGoalsResponse = new GroupedGoalsResponse(
            Arrays.asList(goalResponse), // daily goals
            Collections.emptyList(), // weekly goals
            Collections.emptyList(), // monthly goals
            Collections.emptyList() // annual goals
        );

        // Create sample achievement history response
        AchievementHistoryResponse.AchievementRecord record = new AchievementHistoryResponse.AchievementRecord(
            LocalDate.now(),
            true
        );
        
        achievementHistoryResponse = new AchievementHistoryResponse(
            Arrays.asList(record),
            30, // totalDays
            21, // achievedDays
            0.7 // achievementRate
        );
    }

    @Test
    @WithMockUser
    void getGoals_withNoParameters_shouldReturnActiveGoals() throws Exception {
        when(goalService.getGoalsByDateAndFilter(isNull(), eq("active")))
            .thenReturn(groupedGoalsResponse);

        mockMvc.perform(get("/api/v1/goals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.daily", hasSize(1)))
            .andExpect(jsonPath("$.daily[0].id").value(1))
            .andExpect(jsonPath("$.daily[0].title").value("Exercise Daily"))
            .andExpect(jsonPath("$.daily[0].currentStreak").value(7))
            .andExpect(jsonPath("$.weekly", hasSize(0)));

        verify(goalService).getGoalsByDateAndFilter(isNull(), eq("active"));
    }

    @Test
    @WithMockUser
    void getGoals_withDateAndFilter_shouldReturnFilteredGoals() throws Exception {
        LocalDate testDate = LocalDate.now();
        
        when(goalService.getGoalsByDateAndFilter(eq(testDate), eq("completed")))
            .thenReturn(groupedGoalsResponse);

        mockMvc.perform(get("/api/v1/goals")
                .param("date", testDate.toString())
                .param("filter", "completed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.daily", hasSize(1)));

        verify(goalService).getGoalsByDateAndFilter(eq(testDate), eq("completed"));
    }

    @Test
    @WithMockUser
    void createGoal_withValidRequest_shouldReturnCreatedGoal() throws Exception {
        when(goalService.createGoal(org.mockito.ArgumentMatchers.any(CreateGoalRequest.class))).thenReturn(sampleGoal);

        mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createGoalRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Exercise Daily"))
            .andExpect(jsonPath("$.goalType").value("DAILY"))
            .andExpect(jsonPath("$.isActive").value(true));

        verify(goalService).createGoal(org.mockito.ArgumentMatchers.any(CreateGoalRequest.class));
    }

    @Test
    @WithMockUser
    void createGoal_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateGoalRequest invalidRequest = new CreateGoalRequest(
            "", // Empty title
            null,
            null, // No goal type
            null,
            null
        );

        mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(goalService, never()).createGoal(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void updateGoal_withValidRequest_shouldReturnUpdatedGoal() throws Exception {
        Goal updatedGoal = new Goal();
        updatedGoal.setId(1L);
        updatedGoal.setUserId(sampleGoal.getUserId());
        updatedGoal.setTitle("Exercise Daily - Updated");
        updatedGoal.setDescription("45 minutes of exercise every day");
        updatedGoal.setGoalType(GoalType.DAILY);
        updatedGoal.setIsActive(true);
        updatedGoal.setStartDate(sampleGoal.getStartDate());
        updatedGoal.setEndDate(LocalDate.now().plusDays(30));
        updatedGoal.setCreatedAt(sampleGoal.getCreatedAt());
        updatedGoal.setUpdatedAt(LocalDateTime.now());
        
        when(goalService.updateGoal(eq(1L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class))).thenReturn(updatedGoal);

        mockMvc.perform(put("/api/v1/goals/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateGoalRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Exercise Daily - Updated"))
            .andExpect(jsonPath("$.description").value("45 minutes of exercise every day"));

        verify(goalService).updateGoal(eq(1L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class));
    }

    @Test
    @WithMockUser
    void updateGoal_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(goalService.updateGoal(eq(999L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class)))
            .thenThrow(new TodoNotFoundException("Goal not found with id: 999"));

        mockMvc.perform(put("/api/v1/goals/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateGoalRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Goal not found with id: 999"));

        verify(goalService).updateGoal(eq(999L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class));
    }

    @Test
    @WithMockUser
    void updateGoal_withAccessDenied_shouldReturnForbidden() throws Exception {
        when(goalService.updateGoal(eq(1L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class)))
            .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(put("/api/v1/goals/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateGoalRequest)))
            .andExpect(status().isForbidden());

        verify(goalService).updateGoal(eq(1L), org.mockito.ArgumentMatchers.any(UpdateGoalRequest.class));
    }

    @Test
    @WithMockUser
    void deleteGoal_withValidId_shouldReturnNoContent() throws Exception {
        doNothing().when(goalService).deleteGoal(1L);

        mockMvc.perform(delete("/api/v1/goals/1"))
            .andExpect(status().isNoContent());

        verify(goalService).deleteGoal(1L);
    }

    @Test
    @WithMockUser
    void deleteGoal_withNonExistentId_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException("Goal not found with id: 999"))
            .when(goalService).deleteGoal(999L);

        mockMvc.perform(delete("/api/v1/goals/999"))
            .andExpect(status().isNotFound());

        verify(goalService).deleteGoal(999L);
    }

    @Test
    @WithMockUser
    void toggleAchievement_withNoDate_shouldUseToday() throws Exception {
        doNothing().when(goalService).toggleAchievement(eq(1L), org.mockito.ArgumentMatchers.any(LocalDate.class));

        mockMvc.perform(post("/api/v1/goals/1/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());

        verify(goalService).toggleAchievement(eq(1L), eq(LocalDate.now()));
    }

    @Test
    @WithMockUser
    void toggleAchievement_withSpecificDate_shouldUseProvidedDate() throws Exception {
        LocalDate testDate = LocalDate.now().minusDays(3);
        ToggleAchievementRequest request = new ToggleAchievementRequest(testDate);
        
        doNothing().when(goalService).toggleAchievement(eq(1L), eq(testDate));

        mockMvc.perform(post("/api/v1/goals/1/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(goalService).toggleAchievement(eq(1L), eq(testDate));
    }

    @Test
    @WithMockUser
    void toggleAchievement_withNonExistentGoal_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException("Goal not found with id: 999"))
            .when(goalService).toggleAchievement(eq(999L), org.mockito.ArgumentMatchers.any(LocalDate.class));

        mockMvc.perform(post("/api/v1/goals/999/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());

        verify(goalService).toggleAchievement(eq(999L), org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    @WithMockUser
    void deleteAchievement_withValidDate_shouldReturnNoContent() throws Exception {
        LocalDate testDate = LocalDate.now().minusDays(1);
        doNothing().when(goalService).toggleAchievement(eq(1L), eq(testDate));

        mockMvc.perform(delete("/api/v1/goals/1/achievements")
                .param("date", testDate.toString()))
            .andExpect(status().isNoContent());

        verify(goalService).toggleAchievement(eq(1L), eq(testDate));
    }

    @Test
    @WithMockUser
    void getAchievementHistory_withNoDateRange_shouldReturnAllHistory() throws Exception {
        when(goalService.getAchievementHistory(eq(1L), isNull(), isNull()))
            .thenReturn(achievementHistoryResponse);

        mockMvc.perform(get("/api/v1/goals/1/achievements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.achievements", hasSize(1)))
            .andExpect(jsonPath("$.achievements[0].date").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.achievements[0].achieved").value(true))
            .andExpect(jsonPath("$.totalDays").value(30))
            .andExpect(jsonPath("$.achievedDays").value(21))
            .andExpect(jsonPath("$.achievementRate").value(0.7));

        verify(goalService).getAchievementHistory(eq(1L), isNull(), isNull());
    }

    @Test
    @WithMockUser
    void getAchievementHistory_withDateRange_shouldReturnFilteredHistory() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        
        when(goalService.getAchievementHistory(eq(1L), eq(from), eq(to)))
            .thenReturn(achievementHistoryResponse);

        mockMvc.perform(get("/api/v1/goals/1/achievements")
                .param("from", from.toString())
                .param("to", to.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.achievements", hasSize(1)));

        verify(goalService).getAchievementHistory(eq(1L), eq(from), eq(to));
    }

    @Test
    @WithMockUser
    void getAchievementHistory_withNonExistentGoal_shouldReturnNotFound() throws Exception {
        when(goalService.getAchievementHistory(eq(999L), isNull(), isNull()))
            .thenThrow(new TodoNotFoundException("Goal not found with id: 999"));

        mockMvc.perform(get("/api/v1/goals/999/achievements"))
            .andExpect(status().isNotFound());

        verify(goalService).getAchievementHistory(eq(999L), isNull(), isNull());
    }
}