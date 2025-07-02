package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.RepeatConfigRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateTodoRequest;
import com.zametech.todoapp.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
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
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserContextService userContextService;

    @Mock
    private RepeatService repeatService;

    @InjectMocks
    private TodoService todoService;

    private UUID userId;
    private TodoEntity todoEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        todoEntity = new TodoEntity(
            userId,
            "Test TODO",
            "Test Description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(7),
            null
        );
        todoEntity.setId(1L);
    }

    @Test
    void createTodo_withValidRequest_shouldCreateAndReturnTodo() {
        // Given
        CreateTodoRequest request = new CreateTodoRequest(
            "New TODO",
            "Description",
            TodoPriority.HIGH,
            LocalDate.now().plusDays(3),
            null,
            false,
            null
        );
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(todoEntity);

        // When
        TodoResponse response = todoService.createTodo(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test TODO");
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void createTodo_withRepeatConfig_shouldCreateRepeatableTodo() {
        // Given
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.DAILY,
            1,
            null,
            null,
            LocalDate.now().plusMonths(1)
        );
        CreateTodoRequest request = new CreateTodoRequest(
            "Repeatable TODO",
            "Description",
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            repeatConfig
        );
        
        TodoEntity repeatableTodo = new TodoEntity(
            userId,
            "Repeatable TODO",
            "Description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            LocalDate.now().plusMonths(1),
            null
        );
        repeatableTodo.setId(2L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(repeatableTodo);

        // When
        TodoResponse response = todoService.createTodo(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isRepeatable()).isTrue();
        assertThat(response.repeatConfig()).isNotNull();
        assertThat(response.repeatConfig().repeatType()).isEqualTo(RepeatType.DAILY);
    }

    @Test
    void createTodo_withInvalidParentId_shouldThrowNotFoundException() {
        // Given
        CreateTodoRequest request = new CreateTodoRequest(
            "Child TODO",
            "Description",
            TodoPriority.MEDIUM,
            null,
            999L,
            false,
            null
        );
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.createTodo(request))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void createTodo_withParentFromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        TodoEntity parentTodo = new TodoEntity(
            otherUserId,
            "Parent TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            null
        );
        parentTodo.setId(100L);
        
        CreateTodoRequest request = new CreateTodoRequest(
            "Child TODO",
            "Description",
            TodoPriority.MEDIUM,
            null,
            100L,
            false,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(100L)).thenReturn(Optional.of(parentTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.createTodo(request))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("parent TODO");
    }

    @Test
    void getTodo_withValidId_shouldReturnTodo() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));

        // When
        TodoResponse response = todoService.getTodo(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test TODO");
    }

    @Test
    void getTodo_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.getTodo(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getTodo_fromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));

        // When & Then
        assertThatThrownBy(() -> todoService.getTodo(1L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void getTodos_shouldReturnPagedTodos() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<TodoEntity> todoList = Arrays.asList(todoEntity);
        Page<TodoEntity> todoPage = new PageImpl<>(todoList, pageRequest, 1);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findByUserId(userId, pageRequest)).thenReturn(todoPage);

        // When
        Page<TodoResponse> response = todoService.getTodos(pageRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).title()).isEqualTo("Test TODO");
    }

    @Test
    void getTodosByStatus_shouldReturnFilteredTodos() {
        // Given
        TodoEntity doneTodo = new TodoEntity(
            userId,
            "Done TODO",
            null,
            TodoStatus.DONE,
            TodoPriority.LOW,
            null,
            null
        );
        doneTodo.setId(2L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findByUserIdAndStatus(userId, TodoStatus.DONE))
            .thenReturn(Arrays.asList(doneTodo));

        // When
        List<TodoResponse> response = todoService.getTodosByStatus(TodoStatus.DONE);

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(TodoStatus.DONE);
    }

    @Test
    void updateTodo_withValidRequest_shouldUpdateAndReturnTodo() {
        // Given
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Updated TODO",
            "Updated Description",
            TodoStatus.IN_PROGRESS,
            TodoPriority.HIGH,
            LocalDate.now().plusDays(5),
            null,
            false,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.updateTodo(1L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Updated TODO");
        assertThat(response.status()).isEqualTo(TodoStatus.IN_PROGRESS);
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void updateTodo_withCircularDependency_shouldThrowException() {
        // Given
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Updated TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            1L, // Same as the TODO being updated
            false,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));

        // When & Then
        assertThatThrownBy(() -> todoService.updateTodo(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be its own parent");
    }

    @Test
    void deleteTodo_withValidId_shouldDeleteTodo() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));

        // When
        todoService.deleteTodo(1L);

        // Then
        verify(todoRepository).deleteById(1L);
    }

    @Test
    void deleteTodo_fromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));

        // When & Then
        assertThatThrownBy(() -> todoService.deleteTodo(1L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void getChildTasks_shouldReturnChildTodos() {
        // Given
        TodoEntity childTodo = new TodoEntity(
            userId,
            "Child TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            1L
        );
        childTodo.setId(2L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.findByParentId(1L)).thenReturn(Arrays.asList(childTodo));

        // When
        List<TodoResponse> response = todoService.getChildTasks(1L);

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).parentId()).isEqualTo(1L);
    }

    @Test
    void getRepeatableTodos_shouldReturnOnlyRepeatableTodos() {
        // Given
        TodoEntity repeatableTodo = new TodoEntity(
            userId,
            "Repeatable TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            null,
            true,
            RepeatType.WEEKLY,
            1,
            "1,3,5",
            null,
            null,
            null
        );
        repeatableTodo.setId(3L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findByUserIdAndIsRepeatableTrue(userId))
            .thenReturn(Arrays.asList(repeatableTodo));

        // When
        List<TodoResponse> response = todoService.getRepeatableTodos();

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).isRepeatable()).isTrue();
    }

    @Test
    void toggleTodoStatus_fromTodoToDone_shouldUpdateStatus() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.toggleTodoStatus(1L);

        // Then
        assertThat(response.status()).isEqualTo(TodoStatus.DONE);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.DONE));
    }

    @Test
    void toggleTodoStatus_fromDoneToTodo_shouldUpdateStatus() {
        // Given
        todoEntity.setStatus(TodoStatus.DONE);
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.toggleTodoStatus(1L);

        // Then
        assertThat(response.status()).isEqualTo(TodoStatus.TODO);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.TODO));
    }

    @Test
    void generatePendingRepeatInstances_shouldCreateNewInstances() {
        // Given
        TodoEntity newInstance = new TodoEntity(
            userId,
            "Generated TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(1),
            null
        );
        newInstance.setId(10L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(repeatService.generateAllPendingOccurrences(userId))
            .thenReturn(Arrays.asList(newInstance));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        List<TodoResponse> response = todoService.generatePendingRepeatInstances();

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("Generated TODO");
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void getRepeatInstances_withValidOriginalTodoId_shouldReturnInstances() {
        // Given
        Long originalTodoId = 1L;
        TodoEntity instance1 = new TodoEntity(
            userId,
            "Instance 1",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );
        instance1.setId(10L);
        instance1.setOriginalTodoId(originalTodoId);
        
        TodoEntity instance2 = new TodoEntity(
            userId,
            "Instance 2",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(7),
            null
        );
        instance2.setId(11L);
        instance2.setOriginalTodoId(originalTodoId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.findByOriginalTodoId(originalTodoId))
            .thenReturn(Arrays.asList(instance1, instance2));

        // When
        List<TodoResponse> response = todoService.getRepeatInstances(originalTodoId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).originalTodoId()).isEqualTo(originalTodoId);
        assertThat(response.get(1).originalTodoId()).isEqualTo(originalTodoId);
    }

    @Test
    void getRepeatInstances_withNonExistentOriginalTodoId_shouldThrowNotFoundException() {
        // Given
        Long nonExistentId = 999L;
        when(todoRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.getRepeatInstances(nonExistentId))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getRepeatInstances_fromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        Long originalTodoId = 1L;
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(todoRepository.findById(originalTodoId)).thenReturn(Optional.of(todoEntity));

        // When & Then
        assertThatThrownBy(() -> todoService.getRepeatInstances(originalTodoId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void updateTodo_completingRepeatableTask_shouldGenerateNextOccurrence() {
        // Given
        TodoEntity repeatableTodo = new TodoEntity(
            userId,
            "Daily Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );
        repeatableTodo.setId(1L);
        
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Daily Task",
            null,
            TodoStatus.DONE,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            new RepeatConfigRequest(RepeatType.DAILY, 1, null, null, null)
        );
        
        TodoEntity nextOccurrence = new TodoEntity(
            userId,
            "Daily Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(1),
            null
        );
        nextOccurrence.setOriginalTodoId(1L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(repeatableTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(repeatService.generateNextOccurrence(any(TodoEntity.class))).thenReturn(nextOccurrence);

        // When
        TodoResponse response = todoService.updateTodo(1L, request);

        // Then
        assertThat(response.status()).isEqualTo(TodoStatus.DONE);
        verify(repeatService).generateNextOccurrence(any(TodoEntity.class));
        verify(todoRepository, times(2)).save(any(TodoEntity.class)); // Once for update, once for new instance
    }

    @Test
    void updateTodo_addingRepeatConfig_shouldUpdateRepeatFields() {
        // Given
        TodoEntity nonRepeatableTodo = new TodoEntity(
            userId,
            "Normal Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );
        nonRepeatableTodo.setId(1L);
        
        RepeatConfigRequest repeatConfig = new RepeatConfigRequest(
            RepeatType.WEEKLY,
            1,
            Arrays.asList(1, 3, 5),
            null,
            LocalDate.now().plusMonths(2)
        );
        
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Normal Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            repeatConfig
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(nonRepeatableTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.updateTodo(1L, request);

        // Then
        assertThat(response.isRepeatable()).isTrue();
        assertThat(response.repeatConfig()).isNotNull();
        assertThat(response.repeatConfig().repeatType()).isEqualTo(RepeatType.WEEKLY);
        assertThat(response.repeatConfig().daysOfWeek()).containsExactly(1, 3, 5);
    }

    @Test
    void updateTodo_removingRepeatConfig_shouldClearRepeatFields() {
        // Given
        TodoEntity repeatableTodo = new TodoEntity(
            userId,
            "Repeatable Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );
        repeatableTodo.setId(1L);
        
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Repeatable Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            false,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(repeatableTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.updateTodo(1L, request);

        // Then
        assertThat(response.isRepeatable()).isFalse();
        assertThat(response.repeatConfig()).isNull();
        verify(todoRepository).save(argThat(todo -> 
            !todo.getIsRepeatable() && 
            todo.getRepeatType() == null &&
            todo.getRepeatInterval() == null
        ));
    }

    @Test
    void updateTodo_withParentFromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        TodoEntity parentTodo = new TodoEntity(
            otherUserId,
            "Parent TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            null
        );
        parentTodo.setId(100L);
        
        UpdateTodoRequest request = new UpdateTodoRequest(
            "Updated TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            100L,
            false,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.findById(100L)).thenReturn(Optional.of(parentTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.updateTodo(1L, request))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("parent TODO");
    }

    @Test
    void toggleTodoStatus_completingRepeatInstance_shouldGenerateNewInstance() {
        // Given
        TodoEntity originalTodo = new TodoEntity(
            userId,
            "Daily Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null,
            true,
            RepeatType.DAILY,
            1,
            null,
            null,
            null,
            null
        );
        originalTodo.setId(1L);
        
        TodoEntity repeatInstance = new TodoEntity(
            userId,
            "Daily Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now(),
            null
        );
        repeatInstance.setId(10L);
        repeatInstance.setOriginalTodoId(1L);
        
        TodoEntity nextInstance = new TodoEntity(
            userId,
            "Daily Task",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(1),
            null
        );
        nextInstance.setOriginalTodoId(1L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(10L)).thenReturn(Optional.of(repeatInstance));
        when(todoRepository.findById(1L)).thenReturn(Optional.of(originalTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(repeatService.generateNextOccurrence(originalTodo)).thenReturn(nextInstance);

        // When
        TodoResponse response = todoService.toggleTodoStatus(10L);

        // Then
        assertThat(response.status()).isEqualTo(TodoStatus.DONE);
        verify(repeatService).generateNextOccurrence(originalTodo);
        verify(todoRepository, times(2)).save(any(TodoEntity.class)); // Once for status update, once for new instance
    }

    @Test
    void deleteTodo_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.deleteTodo(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getChildTasks_withParentFromDifferentUser_shouldThrowAccessDeniedException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        TodoEntity parentTodo = new TodoEntity(
            otherUserId,
            "Parent TODO",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            null
        );
        parentTodo.setId(100L);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(100L)).thenReturn(Optional.of(parentTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.getChildTasks(100L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void toggleTodoStatus_fromInProgressToDone_shouldUpdateStatus() {
        // Given
        todoEntity.setStatus(TodoStatus.IN_PROGRESS);
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todoEntity));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        TodoResponse response = todoService.toggleTodoStatus(1L);

        // Then
        assertThat(response.status()).isEqualTo(TodoStatus.DONE);
        verify(todoRepository).save(argThat(todo -> todo.getStatus() == TodoStatus.DONE));
    }
}