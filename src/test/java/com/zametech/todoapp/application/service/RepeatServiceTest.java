package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private RepeatService repeatService;

    private TodoEntity createRepeatableTodo(RepeatType repeatType, Integer interval, String daysOfWeek, 
                                          Integer dayOfMonth, LocalDate dueDate, LocalDate endDate) {
        return new TodoEntity(
            1L, // userId
            "Test Repeatable TODO",
            "Test Description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            dueDate,
            null, // parentId
            true, // isRepeatable
            repeatType,
            interval,
            daysOfWeek,
            dayOfMonth,
            endDate,
            null // originalTodoId
        );
    }

    @Test
    void testCalculateNextDueDate_Daily() {
        // Given
        TodoEntity dailyTodo = createRepeatableTodo(
            RepeatType.DAILY, 1, null, null, 
            LocalDate.of(2025, 1, 1), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(dailyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 1, 2));
    }

    @Test
    void testCalculateNextDueDate_DailyWithInterval() {
        // Given
        TodoEntity dailyTodo = createRepeatableTodo(
            RepeatType.DAILY, 3, null, null, 
            LocalDate.of(2025, 1, 1), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(dailyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 1, 4));
    }

    @Test
    void testCalculateNextDueDate_Weekly() {
        // Given - 月曜日 (1) に設定
        TodoEntity weeklyTodo = createRepeatableTodo(
            RepeatType.WEEKLY, 1, "1", null, 
            LocalDate.of(2025, 1, 6), null // 2025-01-06は月曜日
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(weeklyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 1, 13)); // 次の月曜日
    }

    @Test
    void testCalculateNextDueDate_Weekly_MultipleDays() {
        // Given - 月曜日 (1) と金曜日 (5) に設定
        TodoEntity weeklyTodo = createRepeatableTodo(
            RepeatType.WEEKLY, 1, "1,5", null, 
            LocalDate.of(2025, 1, 6), null // 2025-01-06は月曜日
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(weeklyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 1, 10)); // 次の金曜日
    }

    @Test
    void testCalculateNextDueDate_Monthly() {
        // Given - 毎月15日
        TodoEntity monthlyTodo = createRepeatableTodo(
            RepeatType.MONTHLY, 1, null, 15, 
            LocalDate.of(2025, 1, 15), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(monthlyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 2, 15));
    }

    @Test
    void testCalculateNextDueDate_Monthly_LastDayOfMonth() {
        // Given - 毎月31日 (2月は28日になる)
        TodoEntity monthlyTodo = createRepeatableTodo(
            RepeatType.MONTHLY, 1, null, 31, 
            LocalDate.of(2025, 1, 31), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(monthlyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2025, 2, 28)); // 2月は28日まで
    }

    @Test
    void testCalculateNextDueDate_Yearly() {
        // Given
        TodoEntity yearlyTodo = createRepeatableTodo(
            RepeatType.YEARLY, 1, null, null, 
            LocalDate.of(2025, 1, 1), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(yearlyTodo);

        // Then
        assertThat(nextDueDate).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void testCalculateNextDueDate_Once() {
        // Given
        TodoEntity onceTodo = createRepeatableTodo(
            RepeatType.ONCE, null, null, null, 
            LocalDate.of(2025, 1, 1), null
        );

        // When
        LocalDate nextDueDate = repeatService.calculateNextDueDate(onceTodo);

        // Then
        assertThat(nextDueDate).isNull(); // ONCE は繰り返しなし
    }

    @Test
    void testGenerateNextOccurrence() {
        // Given
        TodoEntity originalTodo = createRepeatableTodo(
            RepeatType.DAILY, 1, null, null, 
            LocalDate.of(2025, 1, 1), null
        );
        originalTodo.setId(1L);

        when(todoRepository.findByOriginalTodoIdAndDueDate(1L, LocalDate.of(2025, 1, 2))).thenReturn(List.of());
        
        // Mock the save operation to return the entity with an ID
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> {
            TodoEntity entity = invocation.getArgument(0);
            entity.setId(2L); // Simulate generated ID
            return entity;
        });

        // When
        TodoEntity nextOccurrence = repeatService.generateNextOccurrence(originalTodo);

        // Then
        assertThat(nextOccurrence).isNotNull();
        assertThat(nextOccurrence.getId()).isEqualTo(2L);
        assertThat(nextOccurrence.getTitle()).isEqualTo("Test Repeatable TODO");
        assertThat(nextOccurrence.getDueDate()).isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(nextOccurrence.getOriginalTodoId()).isEqualTo(1L);
        assertThat(nextOccurrence.getIsRepeatable()).isFalse(); // 生成されたインスタンスは繰り返し不可
    }

    @Test
    void testGenerateNextOccurrence_AlreadyExists() {
        // Given
        TodoEntity originalTodo = createRepeatableTodo(
            RepeatType.DAILY, 1, null, null, 
            LocalDate.of(2025, 1, 1), null
        );
        originalTodo.setId(1L);

        // すでに存在するインスタンス
        TodoEntity existingInstance = new TodoEntity();
        when(todoRepository.findByOriginalTodoIdAndDueDate(1L, LocalDate.of(2025, 1, 2)))
            .thenReturn(List.of(existingInstance));

        // When
        TodoEntity nextOccurrence = repeatService.generateNextOccurrence(originalTodo);

        // Then
        assertThat(nextOccurrence).isNull(); // すでに存在する場合はnull
    }

    @Test
    void testGenerateNextOccurrence_BeyondEndDate() {
        // Given
        TodoEntity originalTodo = createRepeatableTodo(
            RepeatType.DAILY, 1, null, null, 
            LocalDate.of(2025, 1, 1), 
            LocalDate.of(2025, 1, 1) // 終了日が今日
        );
        originalTodo.setId(1L);

        // When
        TodoEntity nextOccurrence = repeatService.generateNextOccurrence(originalTodo);

        // Then
        assertThat(nextOccurrence).isNull(); // 終了日を過ぎているのでnull
    }

    @Test
    void testGenerateAllPendingOccurrences() {
        // Given
        Long userId = 1L;
        TodoEntity repeatableTodo = createRepeatableTodo(
            RepeatType.DAILY, 1, null, null, 
            LocalDate.of(2025, 1, 1), null
        );
        repeatableTodo.setId(1L);

        when(todoRepository.findByUserIdAndIsRepeatableTrue(userId)).thenReturn(List.of(repeatableTodo));
        when(todoRepository.findByOriginalTodoIdAndDueDate(1L, LocalDate.of(2025, 1, 2))).thenReturn(List.of());
        
        // Mock the save operation to return the entity with an ID
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> {
            TodoEntity entity = invocation.getArgument(0);
            entity.setId(2L); // Simulate generated ID
            return entity;
        });

        // When
        List<TodoEntity> pendingOccurrences = repeatService.generateAllPendingOccurrences(userId);

        // Then
        assertThat(pendingOccurrences).hasSize(1);
        assertThat(pendingOccurrences.get(0).getDueDate()).isAfter(LocalDate.of(2025, 1, 1));
    }
}