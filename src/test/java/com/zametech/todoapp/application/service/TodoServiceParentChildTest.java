package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateTodoRequest;
import com.zametech.todoapp.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceParentChildTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private TodoService todoService;

    private static final Long USER_ID = 1L;
    private static final Long PARENT_TODO_ID = 100L;
    private static final Long CHILD_TODO_ID = 200L;
    private static final Long OTHER_USER_ID = 2L;

    private TodoEntity parentTodo;
    private TodoEntity childTodo;

    @BeforeEach
    void setUp() {
        parentTodo = new TodoEntity();
        parentTodo.setId(PARENT_TODO_ID);
        parentTodo.setUserId(USER_ID);
        parentTodo.setTitle("Parent Task");
        parentTodo.setStatus(TodoStatus.TODO);
        parentTodo.setPriority(TodoPriority.HIGH);

        childTodo = new TodoEntity();
        childTodo.setId(CHILD_TODO_ID);
        childTodo.setUserId(USER_ID);
        childTodo.setTitle("Child Task");
        childTodo.setStatus(TodoStatus.TODO);
        childTodo.setPriority(TodoPriority.MEDIUM);
        childTodo.setParentId(null); // Initially no parent
    }

    @Test
    void createTodo_WithParentId_Success() {
        // Given
        CreateTodoRequest request = new CreateTodoRequest(
                "New Child Task",
                "Description",
                TodoPriority.MEDIUM,
                LocalDate.now().plusDays(7),
                PARENT_TODO_ID
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(PARENT_TODO_ID)).thenReturn(Optional.of(parentTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> {
            TodoEntity saved = invocation.getArgument(0);
            saved.setId(CHILD_TODO_ID);
            saved.setParentId(PARENT_TODO_ID);
            saved.setCreatedAt(java.time.ZonedDateTime.now());
            saved.setUpdatedAt(java.time.ZonedDateTime.now());
            return saved;
        });

        // When
        TodoResponse result = todoService.createTodo(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.parentId()).isEqualTo(PARENT_TODO_ID);
        verify(todoRepository).findById(PARENT_TODO_ID);
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void createTodo_WithParentId_ParentNotFound() {
        // Given
        CreateTodoRequest request = new CreateTodoRequest(
                "New Child Task",
                "Description",
                TodoPriority.MEDIUM,
                LocalDate.now().plusDays(7),
                999L
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void createTodo_WithParentId_AccessDenied() {
        // Given
        parentTodo.setUserId(OTHER_USER_ID);
        CreateTodoRequest request = new CreateTodoRequest(
                "New Child Task",
                "Description",
                TodoPriority.MEDIUM,
                LocalDate.now().plusDays(7),
                PARENT_TODO_ID
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(PARENT_TODO_ID)).thenReturn(Optional.of(parentTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to parent TODO with id: " + PARENT_TODO_ID);
    }

    @Test
    void updateTodo_WithParentId_Success() {
        // Given
        UpdateTodoRequest request = new UpdateTodoRequest(
                "Updated Task",
                "Updated Description",
                TodoStatus.IN_PROGRESS,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(7),
                PARENT_TODO_ID
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(CHILD_TODO_ID)).thenReturn(Optional.of(childTodo));
        when(todoRepository.findById(PARENT_TODO_ID)).thenReturn(Optional.of(parentTodo));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(childTodo);

        // When
        TodoResponse result = todoService.updateTodo(CHILD_TODO_ID, request);

        // Then
        assertThat(result).isNotNull();
        verify(todoRepository).findById(PARENT_TODO_ID);
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void updateTodo_PreventCircularDependency() {
        // Given
        UpdateTodoRequest request = new UpdateTodoRequest(
                "Updated Task",
                "Updated Description",
                TodoStatus.IN_PROGRESS,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(7),
                CHILD_TODO_ID  // Trying to set itself as parent
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(CHILD_TODO_ID)).thenReturn(Optional.of(childTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.updateTodo(CHILD_TODO_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A task cannot be its own parent");
    }

    @Test
    void getChildTasks_Success() {
        // Given
        TodoEntity child1 = new TodoEntity();
        child1.setId(201L);
        child1.setUserId(USER_ID);
        child1.setTitle("Child Task 1");
        child1.setParentId(PARENT_TODO_ID);

        TodoEntity child2 = new TodoEntity();
        child2.setId(202L);
        child2.setUserId(USER_ID);
        child2.setTitle("Child Task 2");
        child2.setParentId(PARENT_TODO_ID);

        List<TodoEntity> children = Arrays.asList(child1, child2);

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(PARENT_TODO_ID)).thenReturn(Optional.of(parentTodo));
        when(todoRepository.findByParentId(PARENT_TODO_ID)).thenReturn(children);

        // When
        List<TodoResponse> result = todoService.getChildTasks(PARENT_TODO_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(201L);
        assertThat(result.get(1).id()).isEqualTo(202L);
    }

    @Test
    void getChildTasks_AccessDenied() {
        // Given
        parentTodo.setUserId(OTHER_USER_ID);

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(todoRepository.findById(PARENT_TODO_ID)).thenReturn(Optional.of(parentTodo));

        // When & Then
        assertThatThrownBy(() -> todoService.getChildTasks(PARENT_TODO_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied to TODO with id: " + PARENT_TODO_ID);
    }

    @Test
    void getChildTasks_ParentNotFound() {
        // Given
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> todoService.getChildTasks(999L))
                .isInstanceOf(TodoNotFoundException.class);
    }
}