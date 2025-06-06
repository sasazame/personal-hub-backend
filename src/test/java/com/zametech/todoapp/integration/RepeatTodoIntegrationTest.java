package com.zametech.todoapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.infrastructure.persistence.entity.UserEntity;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.RepeatConfigRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateTodoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepeatTodoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // テストユーザー作成
        testUser = new UserEntity();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        entityManager.persist(testUser);
        entityManager.flush();

        // セキュリティコンテキストにユーザーを設定
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                testUser.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            )
        );
    }

    @Test
    void testCreateDailyRepeatTodo() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Daily Exercise",
            "30 minutes workout",
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Daily Exercise"))
                .andExpect(jsonPath("$.isRepeatable").value(true))
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("DAILY"))
                .andExpect(jsonPath("$.repeatConfig.interval").value(1));

        // データベースから検証
        List<TodoEntity> todos = entityManager.createQuery(
            "SELECT t FROM TodoEntity t WHERE t.userId = :userId AND t.isRepeatable = true", 
            TodoEntity.class)
            .setParameter("userId", testUser.getId())
            .getResultList();

        assert todos.size() == 1;
        assert todos.get(0).getRepeatType() == RepeatType.DAILY;
        assert todos.get(0).getRepeatInterval() == 1;
    }

    @Test
    void testCreateWeeklyRepeatTodo() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.WEEKLY,
            1,
            List.of(1, 3, 5), // Monday, Wednesday, Friday
            null,
            null
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Gym Day",
            "Strength training",
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 6), // Monday
            null,
            true,
            repeatConfig
        );

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("WEEKLY"))
                .andExpect(jsonPath("$.repeatConfig.daysOfWeek", hasSize(3)))
                .andExpect(jsonPath("$.repeatConfig.daysOfWeek", containsInAnyOrder(1, 3, 5)));

        // データベースから検証
        List<TodoEntity> todos = entityManager.createQuery(
            "SELECT t FROM TodoEntity t WHERE t.userId = :userId AND t.repeatType = :repeatType", 
            TodoEntity.class)
            .setParameter("userId", testUser.getId())
            .setParameter("repeatType", RepeatType.WEEKLY)
            .getResultList();

        assert todos.size() == 1;
        assert "1,3,5".equals(todos.get(0).getRepeatDaysOfWeek());
    }

    @Test
    void testCreateMonthlyRepeatTodo() throws Exception {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.MONTHLY,
            1,
            null,
            15, // 15th of each month
            LocalDate.of(2025, 12, 31) // End date
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Monthly Report",
            "Submit monthly report",
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 15),
            null,
            true,
            repeatConfig
        );

        // When & Then
        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repeatConfig.repeatType").value("MONTHLY"))
                .andExpect(jsonPath("$.repeatConfig.dayOfMonth").value(15))
                .andExpect(jsonPath("$.repeatConfig.endDate").value("2025-12-31"));
    }

    @Test
    void testGetRepeatableTodos() throws Exception {
        // Given - 繰り返し可能なTODOをいくつか作成
        TodoEntity repeatableTodo1 = new TodoEntity(
            testUser.getId(),
            "Daily Task",
            "Daily description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );

        TodoEntity repeatableTodo2 = new TodoEntity(
            testUser.getId(),
            "Weekly Task",
            "Weekly description",
            TodoStatus.TODO,
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 6),
            null,
            true,
            RepeatType.WEEKLY,
            1,
            "1,5",
            null,
            null,
            null
        );

        // 非繰り返しTODO
        TodoEntity normalTodo = new TodoEntity(
            testUser.getId(),
            "Normal Task",
            "Normal description",
            TodoStatus.TODO,
            TodoPriority.LOW,
            LocalDate.of(2025, 1, 10),
            null
        );

        entityManager.persist(repeatableTodo1);
        entityManager.persist(repeatableTodo2);
        entityManager.persist(normalTodo);
        entityManager.flush();

        // When & Then
        mockMvc.perform(get("/api/v1/todos/repeatable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].isRepeatable", everyItem(is(true))))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Daily Task", "Weekly Task")));
    }

    @Test
    void testCompleteRepeatableTodoGeneratesNext() throws Exception {
        // Given - 繰り返し可能なTODOを作成
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        CreateTodoRequest createRequest = new CreateTodoRequest(
            "Daily Exercise",
            "30 minutes workout",
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        // TODO作成
        String response = mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long todoId = objectMapper.readTree(response).get("id").asLong();

        // TODO完了
        UpdateTodoRequest updateRequest = new UpdateTodoRequest(
            "Daily Exercise",
            "30 minutes workout",
            TodoStatus.DONE, // 完了にする
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        // When & Then
        mockMvc.perform(put("/api/v1/todos/{id}", todoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // 新しいインスタンスが生成されたか確認
        mockMvc.perform(get("/api/v1/todos/{originalTodoId}/instances", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dueDate").value("2025-01-02"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].isRepeatable").value(false))
                .andExpect(jsonPath("$[0].originalTodoId").value(todoId));
    }

    @Test
    void testGeneratePendingRepeatInstances() throws Exception {
        // Given - 過去の期限のある繰り返しTODOを作成
        TodoEntity overdueTodo = new TodoEntity(
            testUser.getId(),
            "Overdue Task",
            "Should generate new instances",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().minusDays(3), // 3日前
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );

        entityManager.persist(overdueTodo);
        entityManager.flush();

        // When & Then
        mockMvc.perform(post("/api/v1/todos/repeat/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].originalTodoId", everyItem(is(overdueTodo.getId().intValue()))))
                .andExpect(jsonPath("$[*].isRepeatable", everyItem(is(false))));
    }

    @Test
    void testUpdateTodoDisableRepeat() throws Exception {
        // Given - 繰り返し可能なTODOを作成
        TodoEntity repeatableTodo = new TodoEntity(
            testUser.getId(),
            "Repeatable Task",
            "Initially repeatable",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );

        entityManager.persist(repeatableTodo);
        entityManager.flush();

        // 繰り返しを無効化
        UpdateTodoRequest updateRequest = new UpdateTodoRequest(
            "No Longer Repeatable Task",
            "Disabled repeat",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 1),
            null,
            false, // 繰り返し無効
            null
        );

        // When & Then
        mockMvc.perform(put("/api/v1/todos/{id}", repeatableTodo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRepeatable").value(false))
                .andExpect(jsonPath("$.repeatConfig").value(nullValue()));

        // データベースから検証
        entityManager.clear();
        TodoEntity updatedTodo = entityManager.find(TodoEntity.class, repeatableTodo.getId());
        assert !updatedTodo.getIsRepeatable();
        assert updatedTodo.getRepeatType() == null;
    }
}