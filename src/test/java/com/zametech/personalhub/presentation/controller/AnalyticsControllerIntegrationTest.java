package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.AnalyticsService;
import com.zametech.personalhub.presentation.dto.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser(roles = "USER")
    void testGetDashboard() throws Exception {
        // Given
        DashboardResponse mockResponse = createMockDashboardResponse();
        when(analyticsService.getDashboard()).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todoStats.totalTodos").value(10))
                .andExpect(jsonPath("$.todoStats.completedTodos").value(6))
                .andExpect(jsonPath("$.eventStats.totalEvents").value(5))
                .andExpect(jsonPath("$.noteStats.totalNotes").value(8));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetTodoActivity() throws Exception {
        // Given
        TodoActivityResponse mockResponse = createMockTodoActivityResponse();
        when(analyticsService.getTodoActivity(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("days", "30")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorityDistribution.HIGH").value(3))
                .andExpect(jsonPath("$.statusDistribution.DONE").value(6))
                .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetTodoActivityWithDateRange() throws Exception {
        // Given
        TodoActivityResponse mockResponse = createMockTodoActivityResponse();
        when(analyticsService.getTodoActivity(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("dateFrom", "2024-01-01")
                .param("dateTo", "2024-01-31")
                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorityDistribution").exists())
                .andExpect(jsonPath("$.statusDistribution").exists());
    }

    @Test
    void testGetDashboardWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                .contentType("application/json"))
                .andExpect(status().isForbidden());
    }

    private DashboardResponse createMockDashboardResponse() {
        TodoStatsResponse todoStats = new TodoStatsResponse(10, 6, 2, 2, 60.0, 1);
        EventStatsResponse eventStats = new EventStatsResponse(5, 3, 2, 1);
        NoteStatsResponse noteStats = new NoteStatsResponse(8, 2, 5, 10);
        
        List<DailyCount> dailyCompletions = List.of(
            new DailyCount(LocalDate.now().minusDays(1), 2),
            new DailyCount(LocalDate.now(), 1)
        );
        ProductivityStatsResponse productivityStats = new ProductivityStatsResponse(
                dailyCompletions, dailyCompletions, dailyCompletions, 85.5);
        
        return new DashboardResponse(todoStats, eventStats, noteStats, productivityStats);
    }

    private TodoActivityResponse createMockTodoActivityResponse() {
        List<DailyCount> dailyCompletions = List.of(
                new DailyCount(LocalDate.now().minusDays(2), 2),
                new DailyCount(LocalDate.now().minusDays(1), 3),
                new DailyCount(LocalDate.now(), 1)
        );
        
        List<DailyCount> dailyCreations = List.of(
                new DailyCount(LocalDate.now().minusDays(5), 1),
                new DailyCount(LocalDate.now().minusDays(3), 2)
        );
        
        Map<String, Integer> priorityDistribution = new HashMap<>();
        priorityDistribution.put("HIGH", 3);
        priorityDistribution.put("MEDIUM", 4);
        priorityDistribution.put("LOW", 3);
        
        Map<String, Integer> statusDistribution = new HashMap<>();
        statusDistribution.put("DONE", 6);
        statusDistribution.put("IN_PROGRESS", 2);
        statusDistribution.put("TODO", 2);
        
        return new TodoActivityResponse(
                dailyCompletions,
                dailyCreations,
                priorityDistribution,
                statusDistribution,
                2.5
        );
    }
}
