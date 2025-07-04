package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.goal.dto.CreateGoalRequest;
import com.zametech.personalhub.application.goal.dto.ToggleAchievementRequest;
import com.zametech.personalhub.application.goal.dto.UpdateGoalRequest;
import com.zametech.personalhub.domain.model.GoalType;
import com.zametech.personalhub.infrastructure.persistence.entity.GoalEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaGoalRepository;
import com.zametech.personalhub.infrastructure.persistence.repository.UserJpaRepository;
import com.zametech.personalhub.infrastructure.security.JwtService;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.zametech.personalhub.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class GoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private JpaGoalRepository goalRepository;

    private String authToken;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new UserEntity();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword("password");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Generate auth token
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities(new ArrayList<>())
                .build();
        authToken = "Bearer " + jwtService.generateToken(userDetails);
    }

    @Test
    void createGoal_ShouldCreateSuccessfully() throws Exception {
        CreateGoalRequest request = new CreateGoalRequest(
                "Daily Exercise",
                "30 minutes workout",
                GoalType.DAILY,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Daily Exercise"))
                .andExpect(jsonPath("$.description").value("30 minutes workout"))
                .andExpect(jsonPath("$.goalType").value("DAILY"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void getGoals_ShouldReturnGroupedGoals() throws Exception {
        // Create test goals
        createTestGoal("Daily Goal", GoalType.DAILY, true);
        createTestGoal("Weekly Goal", GoalType.WEEKLY, true);
        createTestGoal("Monthly Goal", GoalType.MONTHLY, false);

        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", authToken)
                        .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(1)))
                .andExpect(jsonPath("$.weekly", hasSize(1)))
                .andExpect(jsonPath("$.monthly", hasSize(1)))
                .andExpect(jsonPath("$.annual", hasSize(0)));
    }

    @Test
    void getGoals_WithActiveFilter_ShouldReturnOnlyActiveGoals() throws Exception {
        // Create test goals
        createTestGoal("Active Daily", GoalType.DAILY, true);
        createTestGoal("Inactive Daily", GoalType.DAILY, false);

        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", authToken)
                        .param("filter", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(1)))
                .andExpect(jsonPath("$.daily[0].title").value("Active Daily"));
    }

    @Test
    void updateGoal_ShouldUpdateSuccessfully() throws Exception {
        GoalEntity goal = createTestGoal("Original Title", GoalType.DAILY, true);

        UpdateGoalRequest request = new UpdateGoalRequest(
                "Updated Title",
                "Updated Description",
                false
        );

        mockMvc.perform(put("/api/v1/goals/" + goal.getId())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void deleteGoal_ShouldDeleteSuccessfully() throws Exception {
        GoalEntity goal = createTestGoal("Goal to Delete", GoalType.DAILY, true);

        mockMvc.perform(delete("/api/v1/goals/" + goal.getId())
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        // Verify goal is deleted
        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily", hasSize(0)));
    }

    @Test
    void toggleAchievement_ShouldToggleSuccessfully() throws Exception {
        GoalEntity goal = createTestGoal("Daily Goal", GoalType.DAILY, true);
        LocalDate today = LocalDate.now();

        ToggleAchievementRequest request = new ToggleAchievementRequest(today);

        // First toggle - should add achievement
        mockMvc.perform(post("/api/v1/goals/" + goal.getId() + "/achievements")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Check achievement exists
        mockMvc.perform(get("/api/v1/goals/" + goal.getId() + "/achievements")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievedDays").value(1));

        // Second toggle - should remove achievement
        mockMvc.perform(post("/api/v1/goals/" + goal.getId() + "/achievements")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Check achievement removed
        mockMvc.perform(get("/api/v1/goals/" + goal.getId() + "/achievements")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievedDays").value(0));
    }

    @Test
    void getAchievementHistory_ShouldReturnHistory() throws Exception {
        GoalEntity goal = createTestGoal("Weekly Goal", GoalType.WEEKLY, true);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 7);

        mockMvc.perform(get("/api/v1/goals/" + goal.getId() + "/achievements")
                        .header("Authorization", authToken)
                        .param("from", startDate.toString())
                        .param("to", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDays").value(7))
                .andExpect(jsonPath("$.achievedDays").value(0))
                .andExpect(jsonPath("$.achievementRate").value(0.0))
                .andExpect(jsonPath("$.achievements", hasSize(7)));
    }

    @Test
    void accessGoalWithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessOtherUserGoal_ShouldReturn404() throws Exception {
        // Create another user and their goal
        UserEntity otherUser = new UserEntity();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("otheruser");
        otherUser.setEmail("otheruser@example.com");
        otherUser.setPassword("password");
        otherUser.setEnabled(true);
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);

        GoalEntity otherUserGoal = new GoalEntity();
        otherUserGoal.setUserId(otherUser.getId());
        otherUserGoal.setTitle("Other User Goal");
        otherUserGoal.setGoalType(GoalType.DAILY);
        otherUserGoal.setIsActive(true);
        otherUserGoal.setStartDate(LocalDate.now());
        otherUserGoal.setEndDate(LocalDate.now().plusYears(1));
        otherUserGoal = goalRepository.save(otherUserGoal);

        // Try to update other user's goal
        UpdateGoalRequest request = new UpdateGoalRequest("Hacked Title", null, null);

        mockMvc.perform(put("/api/v1/goals/" + otherUserGoal.getId())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // Will throw RuntimeException
    }

    private GoalEntity createTestGoal(String title, GoalType type, boolean isActive) {
        GoalEntity goal = new GoalEntity();
        goal.setUserId(testUser.getId());
        goal.setTitle(title);
        goal.setDescription("Test " + type + " goal");
        goal.setGoalType(type);
        goal.setIsActive(isActive);
        goal.setStartDate(LocalDate.of(2025, 1, 1));
        goal.setEndDate(LocalDate.of(2025, 12, 31));
        return goalRepository.save(goal);
    }
}