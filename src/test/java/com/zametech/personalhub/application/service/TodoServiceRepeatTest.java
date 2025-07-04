package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.RepeatType;
import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.domain.repository.TodoRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;
import com.zametech.personalhub.presentation.dto.request.CreateTodoRequest;
import com.zametech.personalhub.presentation.dto.request.RepeatConfigRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateTodoRequest;
import com.zametech.personalhub.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceRepeatTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private RepeatService repeatService;

    @InjectMocks
    private TodoService todoService;

    @Test
    void testCreateTodo_WithRepeatConfig() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.DAILY,
            1,
            null,
            null,
            null
        );

        CreateTodoRequest request = new CreateTodoRequest(
            "Daily Task",
            "Daily task description",
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        TodoEntity savedTodo = new TodoEntity();
        savedTodo.setId(1L);
        savedTodo.setUserId(userId);
        savedTodo.setTitle("Daily Task");
        savedTodo.setIsRepeatable(true);
        savedTodo.setRepeatType(RepeatType.DAILY);

        when(todoRepository.save(any(TodoEntity.class))).thenReturn(savedTodo);

        // When
        TodoResponse response = todoService.createTodo(request);

        // Then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Daily Task");
        assertThat(response.isRepeatable()).isTrue();

        verify(todoRepository).save(argThat(todo -> 
            todo.getIsRepeatable() && 
            todo.getRepeatType() == RepeatType.DAILY &&
            todo.getRepeatInterval() == 1
        ));
    }

    @Test
    void testCreateTodo_WithoutRepeatConfig() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        CreateTodoRequest request = new CreateTodoRequest(
            "Normal Task",
            "Normal task description",
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 1),
            null,
            false,
            null
        );

        TodoEntity savedTodo = new TodoEntity();
        savedTodo.setId(1L);
        savedTodo.setUserId(userId);
        savedTodo.setTitle("Normal Task");
        savedTodo.setIsRepeatable(false);

        when(todoRepository.save(any(TodoEntity.class))).thenReturn(savedTodo);

        // When
        TodoResponse response = todoService.createTodo(request);

        // Then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Normal Task");
        assertThat(response.isRepeatable()).isFalse();

        verify(todoRepository).save(argThat(todo -> !todo.getIsRepeatable()));
    }

    @Test
    void testUpdateTodo_EnableRepeat() {
        // Given
        Long todoId = 1L;
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity existingTodo = new TodoEntity();
        existingTodo.setId(todoId);
        existingTodo.setUserId(userId);
        existingTodo.setTitle("Task");
        existingTodo.setIsRepeatable(false);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(existingTodo);

        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.WEEKLY,
            1,
            List.of(1, 5), // Monday and Friday
            null,
            null
        );

        UpdateTodoRequest request = new UpdateTodoRequest(
            "Updated Task",
            "Updated description",
            TodoStatus.TODO,
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            repeatConfig
        );

        // When
        TodoResponse response = todoService.updateTodo(todoId, request);

        // Then
        verify(todoRepository).save(argThat(todo -> 
            todo.getIsRepeatable() && 
            todo.getRepeatType() == RepeatType.WEEKLY &&
            "1,5".equals(todo.getRepeatDaysOfWeek())
        ));
    }

    @Test
    void testUpdateTodo_DisableRepeat() {
        // Given
        Long todoId = 1L;
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity existingTodo = new TodoEntity();
        existingTodo.setId(todoId);
        existingTodo.setUserId(userId);
        existingTodo.setTitle("Task");
        existingTodo.setIsRepeatable(true);
        existingTodo.setRepeatType(RepeatType.DAILY);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(existingTodo);

        UpdateTodoRequest request = new UpdateTodoRequest(
            "Updated Task",
            "Updated description",
            TodoStatus.TODO,
            TodoPriority.HIGH,
            LocalDate.of(2025, 1, 1),
            null,
            false,
            null
        );

        // When
        TodoResponse response = todoService.updateTodo(todoId, request);

        // Then
        verify(todoRepository).save(argThat(todo -> 
            !todo.getIsRepeatable() && 
            todo.getRepeatType() == null
        ));
    }

    @Test
    void testUpdateTodo_CompletionGeneratesNext() {
        // Given
        Long todoId = 1L;
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity existingTodo = new TodoEntity();
        existingTodo.setId(todoId);
        existingTodo.setUserId(userId);
        existingTodo.setTitle("Repeatable Task");
        existingTodo.setIsRepeatable(true);
        existingTodo.setRepeatType(RepeatType.DAILY);
        existingTodo.setOriginalTodoId(null); // 元のTODO

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(existingTodo);

        TodoEntity nextInstance = new TodoEntity();
        when(repeatService.generateNextOccurrence(existingTodo)).thenReturn(nextInstance);

        UpdateTodoRequest request = new UpdateTodoRequest(
            "Repeatable Task",
            "Description",
            TodoStatus.DONE, // 完了にする
            TodoPriority.MEDIUM,
            LocalDate.of(2025, 1, 1),
            null,
            true,
            new RepeatConfigRequest(RepeatType.DAILY, 1, null, null, null)
        );

        // When
        todoService.updateTodo(todoId, request);

        // Then
        verify(repeatService).generateNextOccurrence(existingTodo);
        verify(todoRepository, times(2)).save(any(TodoEntity.class)); // 元のTODOと次のインスタンス
    }

    @Test
    void testGetRepeatableTodos() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity repeatableTodo = new TodoEntity();
        repeatableTodo.setId(1L);
        repeatableTodo.setUserId(userId);
        repeatableTodo.setTitle("Repeatable Task");
        repeatableTodo.setIsRepeatable(true);

        when(todoRepository.findByUserIdAndIsRepeatableTrue(userId)).thenReturn(List.of(repeatableTodo));

        // When
        List<TodoResponse> responses = todoService.getRepeatableTodos();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Repeatable Task");
        assertThat(responses.get(0).isRepeatable()).isTrue();
    }

    @Test
    void testGetRepeatInstances() {
        // Given
        Long originalTodoId = 1L;
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity originalTodo = new TodoEntity();
        originalTodo.setId(originalTodoId);
        originalTodo.setUserId(userId);
        originalTodo.setTitle("Original Task");

        TodoEntity instance1 = new TodoEntity();
        instance1.setId(2L);
        instance1.setUserId(userId);
        instance1.setTitle("Original Task");
        instance1.setOriginalTodoId(originalTodoId);

        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.of(originalTodo));
        when(todoRepository.findByOriginalTodoId(originalTodoId)).thenReturn(List.of(instance1));

        // When
        List<TodoResponse> responses = todoService.getRepeatInstances(originalTodoId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).originalTodoId()).isEqualTo(originalTodoId);
    }

    @Test
    void testGetRepeatInstances_AccessDenied() {
        // Given
        Long originalTodoId = 1L;
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity originalTodo = new TodoEntity();
        originalTodo.setId(originalTodoId);
        originalTodo.setUserId(otherUserId); // 他のユーザーのTODO

        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.of(originalTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.getRepeatInstances(originalTodoId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetRepeatInstances_NotFound() {
        // Given
        Long originalTodoId = 1L;
        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.getRepeatInstances(originalTodoId))
            .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void testGeneratePendingRepeatInstances() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(userId);

        TodoEntity newInstance = new TodoEntity();
        newInstance.setId(2L);
        newInstance.setUserId(userId);
        newInstance.setTitle("Generated Instance");

        when(repeatService.generateAllPendingOccurrences(userId)).thenReturn(List.of(newInstance));
        when(todoRepository.save(newInstance)).thenReturn(newInstance);

        // When
        List<TodoResponse> responses = todoService.generatePendingRepeatInstances();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Generated Instance");
        verify(repeatService).generateAllPendingOccurrences(userId);
        verify(todoRepository).save(newInstance);
    }
}