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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoOwnershipServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserContextService userContextService;
    
    @Mock
    private RepeatService repeatService;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, userContextService, repeatService);
    }

    @Test
    void shouldCreateTodoWithCurrentUserAsOwner() {
        Long currentUserId = 1L;
        CreateTodoRequest request = new CreateTodoRequest(
                "Test Todo",
                "Test Description",
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1),
                null,
                false,
                null
        );

        TodoEntity savedTodo = new TodoEntity(
                currentUserId,
                "Test Todo",
                "Test Description",
                TodoStatus.TODO,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1)
        );
        savedTodo.setId(1L);
        savedTodo.setCreatedAt(ZonedDateTime.now());
        savedTodo.setUpdatedAt(ZonedDateTime.now());

        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(savedTodo);

        TodoResponse response = todoService.createTodo(request);

        assertNotNull(response);
        assertEquals("Test Todo", response.title());
        assertEquals(TodoStatus.TODO, response.status());
        verify(userContextService).getCurrentUserId();
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void shouldGetTodoWhenUserIsOwner() {
        Long todoId = 1L;
        Long currentUserId = 1L;

        TodoEntity todo = new TodoEntity(
                currentUserId,
                "Test Todo",
                "Test Description",
                TodoStatus.TODO,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1)
        );
        todo.setId(todoId);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);

        TodoResponse response = todoService.getTodo(todoId);

        assertNotNull(response);
        assertEquals("Test Todo", response.title());
        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenUserIsNotOwner() {
        Long todoId = 1L;
        Long todoOwnerId = 1L;
        Long currentUserId = 2L;

        TodoEntity todo = new TodoEntity(
                todoOwnerId,
                "Test Todo",
                "Test Description",
                TodoStatus.TODO,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1)
        );
        todo.setId(todoId);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);

        assertThrows(AccessDeniedException.class, () -> {
            todoService.getTodo(todoId);
        });

        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
    }

    @Test
    void shouldGetTodosOnlyForCurrentUser() {
        Long currentUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        TodoEntity todo1 = new TodoEntity(
                currentUserId,
                "Todo 1",
                "Description 1",
                TodoStatus.TODO,
                TodoPriority.HIGH,
                LocalDate.now()
        );
        todo1.setId(1L);

        TodoEntity todo2 = new TodoEntity(
                currentUserId,
                "Todo 2",
                "Description 2",
                TodoStatus.DONE,
                TodoPriority.LOW,
                LocalDate.now()
        );
        todo2.setId(2L);

        Page<TodoEntity> todoPage = new PageImpl<>(List.of(todo1, todo2), pageable, 2);

        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);
        when(todoRepository.findByUserId(currentUserId, pageable)).thenReturn(todoPage);

        Page<TodoResponse> response = todoService.getTodos(pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Todo 1", response.getContent().get(0).title());
        assertEquals("Todo 2", response.getContent().get(1).title());
        verify(userContextService).getCurrentUserId();
        verify(todoRepository).findByUserId(currentUserId, pageable);
    }

    @Test
    void shouldGetTodosByStatusOnlyForCurrentUser() {
        Long currentUserId = 1L;
        TodoStatus status = TodoStatus.TODO;

        TodoEntity todo = new TodoEntity(
                currentUserId,
                "Todo 1",
                "Description 1",
                TodoStatus.TODO,
                TodoPriority.HIGH,
                LocalDate.now()
        );
        todo.setId(1L);

        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);
        when(todoRepository.findByUserIdAndStatus(currentUserId, status)).thenReturn(List.of(todo));

        List<TodoResponse> response = todoService.getTodosByStatus(status);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Todo 1", response.get(0).title());
        verify(userContextService).getCurrentUserId();
        verify(todoRepository).findByUserIdAndStatus(currentUserId, status);
    }

    @Test
    void shouldUpdateTodoWhenUserIsOwner() {
        Long todoId = 1L;
        Long currentUserId = 1L;

        TodoEntity existingTodo = new TodoEntity(
                currentUserId,
                "Old Title",
                "Old Description",
                TodoStatus.TODO,
                TodoPriority.LOW,
                LocalDate.now()
        );
        existingTodo.setId(todoId);

        UpdateTodoRequest request = new UpdateTodoRequest(
                "New Title",
                "New Description",
                TodoStatus.DONE,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1),
                null,
                false,
                null
        );

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(existingTodo);

        TodoResponse response = todoService.updateTodo(todoId, request);

        assertNotNull(response);
        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenUpdatingTodoUserIsNotOwner() {
        Long todoId = 1L;
        Long todoOwnerId = 1L;
        Long currentUserId = 2L;

        TodoEntity existingTodo = new TodoEntity(
                todoOwnerId,
                "Old Title",
                "Old Description",
                TodoStatus.TODO,
                TodoPriority.LOW,
                LocalDate.now()
        );
        existingTodo.setId(todoId);

        UpdateTodoRequest request = new UpdateTodoRequest(
                "New Title",
                "New Description",
                TodoStatus.DONE,
                TodoPriority.HIGH,
                LocalDate.now().plusDays(1),
                null,
                false,
                null
        );

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);

        assertThrows(AccessDeniedException.class, () -> {
            todoService.updateTodo(todoId, request);
        });

        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
        verify(todoRepository, never()).save(any(TodoEntity.class));
    }

    @Test
    void shouldDeleteTodoWhenUserIsOwner() {
        Long todoId = 1L;
        Long currentUserId = 1L;

        TodoEntity existingTodo = new TodoEntity(
                currentUserId,
                "Title",
                "Description",
                TodoStatus.TODO,
                TodoPriority.LOW,
                LocalDate.now()
        );
        existingTodo.setId(todoId);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);

        todoService.deleteTodo(todoId);

        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
        verify(todoRepository).deleteById(todoId);
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenDeletingTodoUserIsNotOwner() {
        Long todoId = 1L;
        Long todoOwnerId = 1L;
        Long currentUserId = 2L;

        TodoEntity existingTodo = new TodoEntity(
                todoOwnerId,
                "Title",
                "Description",
                TodoStatus.TODO,
                TodoPriority.LOW,
                LocalDate.now()
        );
        existingTodo.setId(todoId);

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(existingTodo));
        when(userContextService.getCurrentUserId()).thenReturn(currentUserId);

        assertThrows(AccessDeniedException.class, () -> {
            todoService.deleteTodo(todoId);
        });

        verify(todoRepository).findById(todoId);
        verify(userContextService).getCurrentUserId();
        verify(todoRepository, never()).deleteById(todoId);
    }
}