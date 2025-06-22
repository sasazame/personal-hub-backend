package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceToggleStatusTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private RepeatService repeatService;

    @InjectMocks
    private TodoService todoService;

    @Test
    void testToggleTodoStatus_TodoToDone() {
        // Given
        Long todoId = 1L;
        UUID userId = UUID.randomUUID();
        
        TodoEntity todoEntity = createTodoEntity(todoId, userId.getMostSignificantBits(), TodoStatus.TODO, null);
        TodoEntity updatedEntity = createTodoEntity(todoId, userId.getMostSignificantBits(), TodoStatus.DONE, null);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(userId.getMostSignificantBits());
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updatedEntity);

        // When
        TodoResponse result = todoService.toggleTodoStatus(todoId);

        // Then
        assertThat(result.status()).isEqualTo(TodoStatus.DONE);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.DONE));
    }

    @Test
    void testToggleTodoStatus_DoneToTodo() {
        // Given
        Long todoId = 1L;
        Long userId = 1L;
        
        TodoEntity todoEntity = createTodoEntity(todoId, userId, TodoStatus.DONE, null);
        TodoEntity updatedEntity = createTodoEntity(todoId, userId, TodoStatus.TODO, null);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updatedEntity);

        // When
        TodoResponse result = todoService.toggleTodoStatus(todoId);

        // Then
        assertThat(result.status()).isEqualTo(TodoStatus.TODO);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.TODO));
    }

    @Test
    void testToggleTodoStatus_InProgressToDone() {
        // Given
        Long todoId = 1L;
        Long userId = 1L;
        
        TodoEntity todoEntity = createTodoEntity(todoId, userId, TodoStatus.IN_PROGRESS, null);
        TodoEntity updatedEntity = createTodoEntity(todoId, userId, TodoStatus.DONE, null);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updatedEntity);

        // When
        TodoResponse result = todoService.toggleTodoStatus(todoId);

        // Then
        assertThat(result.status()).isEqualTo(TodoStatus.DONE);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.DONE));
    }

    @Test
    void testToggleTodoStatus_RepeatInstanceCompletion() {
        // Given
        Long todoId = 1L;
        Long originalTodoId = 2L;
        Long userId = 1L;
        
        TodoEntity repeatInstance = createTodoEntity(todoId, userId, TodoStatus.TODO, originalTodoId);
        TodoEntity originalTodo = createRepeatableTodoEntity(originalTodoId, userId);
        TodoEntity updatedInstance = createTodoEntity(todoId, userId, TodoStatus.DONE, originalTodoId);
        TodoEntity nextInstance = createTodoEntity(3L, userId, TodoStatus.TODO, originalTodoId);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(repeatInstance));
        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.of(originalTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updatedInstance);
        when(repeatService.generateNextOccurrence(originalTodo)).thenReturn(nextInstance);

        // When
        TodoResponse result = todoService.toggleTodoStatus(todoId);

        // Then
        assertThat(result.status()).isEqualTo(TodoStatus.DONE);
        verify(repeatService).generateNextOccurrence(originalTodo);
        verify(todoRepository, times(2)).save(any(TodoEntity.class)); // Update instance + save new instance
    }

    @Test
    void testToggleTodoStatus_TodoNotFound() {
        // Given
        Long todoId = 999L;
        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.toggleTodoStatus(todoId))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void testToggleTodoStatus_AccessDenied() {
        // Given
        Long todoId = 1L;
        Long currentUserId = 1L;
        Long otherUserId = 2L;
        
        TodoEntity todoEntity = createTodoEntity(todoId, otherUserId, TodoStatus.TODO, null);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(currentUserId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todoEntity));

        // When & Then
        assertThatThrownBy(() -> todoService.toggleTodoStatus(todoId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to TODO with id: " + todoId);
    }

    @Test
    void testToggleTodoStatus_RepeatInstanceWithoutOriginal() {
        // Given
        Long todoId = 1L;
        Long originalTodoId = 2L;
        Long userId = 1L;
        
        TodoEntity repeatInstance = createTodoEntity(todoId, userId, TodoStatus.TODO, originalTodoId);
        TodoEntity updatedInstance = createTodoEntity(todoId, userId, TodoStatus.DONE, originalTodoId);
        
        when(userContextService.getCurrentUserIdAsLong()).thenReturn(userId);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(repeatInstance));
        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.empty()); // Original not found
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updatedInstance);

        // When
        TodoResponse result = todoService.toggleTodoStatus(todoId);

        // Then
        assertThat(result.status()).isEqualTo(TodoStatus.DONE);
        verify(repeatService, never()).generateNextOccurrence(any());
        verify(todoRepository, times(1)).save(any(TodoEntity.class)); // Only update instance
    }

    private TodoEntity createTodoEntity(Long id, Long userId, TodoStatus status, Long originalTodoId) {
        TodoEntity entity = new TodoEntity();
        entity.setId(id);
        entity.setTitle("Test TODO");
        entity.setDescription("Test description");
        entity.setStatus(status);
        entity.setUserId(userId);
        entity.setOriginalTodoId(originalTodoId);
        entity.setCreatedAt(ZonedDateTime.now());
        entity.setUpdatedAt(ZonedDateTime.now());
        return entity;
    }

    private TodoEntity createRepeatableTodoEntity(Long id, Long userId) {
        TodoEntity entity = createTodoEntity(id, userId, TodoStatus.TODO, null);
        entity.setIsRepeatable(true);
        return entity;
    }
}