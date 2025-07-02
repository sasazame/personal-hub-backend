package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.AnalyticsService;
import com.zametech.todoapp.presentation.dto.response.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AnalyticsController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    private DashboardResponse dashboardResponse;
    private TodoActivityResponse todoActivityResponse;

    @BeforeEach
    void setUp() {
        // Create sample dashboard response
        TodoStatsResponse todoStats = new TodoStatsResponse(
            100L, // totalTodos
            75L,  // completedTodos
            10L,  // inProgressTodos
            15L,  // pendingTodos
            0.75, // completionRate
            5L    // overdueCount
        );
        
        NoteStatsResponse noteStats = new NoteStatsResponse(
            50L,  // totalNotes
            10L,  // notesThisWeek
            20L,  // notesThisMonth
            5L    // totalTags
        );
        
        EventStatsResponse eventStats = new EventStatsResponse(
            20L,  // totalEvents
            15L,  // upcomingEvents
            5L,   // pastEvents
            2L    // todayEvents
        );
        
        List<DailyCount> dailyCounts = Arrays.asList(
            new DailyCount(LocalDate.now().minusDays(2), 5),
            new DailyCount(LocalDate.now().minusDays(1), 8),
            new DailyCount(LocalDate.now(), 3)
        );
        
        ProductivityStatsResponse productivityStats = new ProductivityStatsResponse(
            dailyCounts, // dailyTodoCompletions
            dailyCounts, // dailyEventCounts
            dailyCounts, // dailyNoteCreations
            0.85         // weeklyProductivityScore
        );
        
        dashboardResponse = new DashboardResponse(
            todoStats,
            eventStats,
            noteStats,
            productivityStats
        );

        // Create sample todo activity response
        Map<String, Integer> priorityDistribution = new HashMap<>();
        priorityDistribution.put("HIGH", 5);
        priorityDistribution.put("MEDIUM", 8);
        priorityDistribution.put("LOW", 3);
        
        Map<String, Integer> statusDistribution = new HashMap<>();
        statusDistribution.put("COMPLETED", 16);
        statusDistribution.put("IN_PROGRESS", 4);
        statusDistribution.put("PENDING", 10);
        
        todoActivityResponse = new TodoActivityResponse(
            dailyCounts,          // dailyCompletions
            dailyCounts,          // dailyCreations
            priorityDistribution, // priorityDistribution
            statusDistribution,   // statusDistribution
            2.5                   // averageCompletionTimeInDays
        );
    }

    @Test
    @WithMockUser
    void getDashboard_shouldReturnDashboardData() throws Exception {
        when(analyticsService.getDashboard()).thenReturn(dashboardResponse);

        mockMvc.perform(get("/api/v1/analytics/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todoStats.totalTodos").value(100))
            .andExpect(jsonPath("$.todoStats.completedTodos").value(75))
            .andExpect(jsonPath("$.todoStats.pendingTodos").value(15))
            .andExpect(jsonPath("$.todoStats.inProgressTodos").value(10))
            .andExpect(jsonPath("$.todoStats.overdueCount").value(5))
            .andExpect(jsonPath("$.todoStats.completionRate").value(0.75))
            .andExpect(jsonPath("$.noteStats.totalNotes").value(50))
            .andExpect(jsonPath("$.noteStats.notesThisWeek").value(10))
            .andExpect(jsonPath("$.noteStats.notesThisMonth").value(20))
            .andExpect(jsonPath("$.noteStats.totalTags").value(5))
            .andExpect(jsonPath("$.eventStats.totalEvents").value(20))
            .andExpect(jsonPath("$.eventStats.upcomingEvents").value(15))
            .andExpect(jsonPath("$.eventStats.pastEvents").value(5))
            .andExpect(jsonPath("$.eventStats.todayEvents").value(2))
            .andExpect(jsonPath("$.productivityStats.weeklyProductivityScore").value(0.85))
            .andExpect(jsonPath("$.productivityStats.dailyTodoCompletions", hasSize(3)));

        verify(analyticsService).getDashboard();
    }

    @Test
    @WithMockUser
    void getTodoActivity_withDefaultParameters_shouldUse30Days() throws Exception {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        when(analyticsService.getTodoActivity(eq(startDate), eq(endDate)))
            .thenReturn(todoActivityResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dailyCompletions", hasSize(3)))
            .andExpect(jsonPath("$.dailyCreations", hasSize(3)))
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5))
            .andExpect(jsonPath("$.dailyCompletions[0].date").value(LocalDate.now().minusDays(2).toString()))
            .andExpect(jsonPath("$.dailyCompletions[0].count").value(5))
            .andExpect(jsonPath("$.priorityDistribution.HIGH").value(5))
            .andExpect(jsonPath("$.statusDistribution.COMPLETED").value(16));

        verify(analyticsService).getTodoActivity(eq(startDate), eq(endDate));
    }

    @Test
    @WithMockUser
    void getTodoActivity_withCustomDays_shouldUseProvidedDays() throws Exception {
        int days = 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        when(analyticsService.getTodoActivity(eq(startDate), eq(endDate)))
            .thenReturn(todoActivityResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("days", String.valueOf(days)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5));

        verify(analyticsService).getTodoActivity(eq(startDate), eq(endDate));
    }

    @Test
    @WithMockUser
    void getTodoActivity_withDateRange_shouldUseProvidedDates() throws Exception {
        LocalDate dateFrom = LocalDate.now().minusDays(15);
        LocalDate dateTo = LocalDate.now().minusDays(5);
        
        when(analyticsService.getTodoActivity(eq(dateFrom), eq(dateTo)))
            .thenReturn(todoActivityResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("dateFrom", dateFrom.toString())
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5));

        verify(analyticsService).getTodoActivity(eq(dateFrom), eq(dateTo));
    }

    @Test
    @WithMockUser
    void getTodoActivity_withOnlyDateFrom_shouldUseFromDateToToday() throws Exception {
        LocalDate dateFrom = LocalDate.now().minusDays(10);
        LocalDate dateTo = LocalDate.now();
        
        when(analyticsService.getTodoActivity(eq(dateFrom), eq(dateTo)))
            .thenReturn(todoActivityResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("dateFrom", dateFrom.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5));

        verify(analyticsService).getTodoActivity(eq(dateFrom), eq(dateTo));
    }

    @Test
    @WithMockUser  
    void getTodoActivity_withOnlyDateTo_shouldUseDaysParameter() throws Exception {
        LocalDate dateTo = LocalDate.now().minusDays(5);
        LocalDate dateFrom = dateTo.minusDays(30); // Default 30 days
        
        when(analyticsService.getTodoActivity(eq(dateFrom), eq(dateTo)))
            .thenReturn(todoActivityResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity")
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(2.5));

        verify(analyticsService).getTodoActivity(eq(dateFrom), eq(dateTo));
    }

    @Test
    @WithMockUser
    void getTodoActivity_withEmptyResponse_shouldReturnEmptyLists() throws Exception {
        TodoActivityResponse emptyResponse = new TodoActivityResponse(
            Arrays.asList(),  // empty dailyCompletions
            Arrays.asList(),  // empty dailyCreations
            new HashMap<>(),  // empty priorityDistribution
            new HashMap<>(),  // empty statusDistribution
            0.0               // averageCompletionTimeInDays
        );
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        when(analyticsService.getTodoActivity(eq(startDate), eq(endDate)))
            .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/analytics/todos/activity"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dailyCompletions", hasSize(0)))
            .andExpect(jsonPath("$.dailyCreations", hasSize(0)))
            .andExpect(jsonPath("$.averageCompletionTimeInDays").value(0.0));

        verify(analyticsService).getTodoActivity(eq(startDate), eq(endDate));
    }
}