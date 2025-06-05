package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateTodoRequest;
import com.zametech.todoapp.presentation.dto.response.TodoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TODOサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserContextService userContextService;

    /**
     * TODO作成
     */
    @Transactional
    public TodoResponse createTodo(CreateTodoRequest request) {
        log.debug("Creating new TODO: {}", request.title());
        
        Long currentUserId = userContextService.getCurrentUserId();
        
        // Validate parent task if provided
        if (request.parentId() != null) {
            TodoEntity parentTodo = todoRepository.findById(request.parentId())
                .orElseThrow(() -> new TodoNotFoundException(request.parentId()));
            
            // Ensure parent task belongs to the same user
            if (!parentTodo.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied to parent TODO with id: " + request.parentId());
            }
        }
        
        TodoEntity todo = new TodoEntity(
            currentUserId,
            request.title(),
            request.description(),
            TodoStatus.TODO,
            request.priority(),
            request.dueDate(),
            request.parentId()
        );
        
        TodoEntity saved = todoRepository.save(todo);
        log.info("Created TODO with id: {} for user: {}", saved.getId(), currentUserId);
        
        return TodoResponse.from(saved);
    }

    /**
     * TODO取得（ID指定）
     */
    public TodoResponse getTodo(Long id) {
        log.debug("Getting TODO with id: {}", id);
        
        TodoEntity todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        
        Long currentUserId = userContextService.getCurrentUserId();
        if (!todo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to TODO with id: " + id);
        }
            
        return TodoResponse.from(todo);
    }

    /**
     * TODO一覧取得（ページング）
     */
    public Page<TodoResponse> getTodos(Pageable pageable) {
        log.debug("Getting TODO list with pageable: {}", pageable);
        
        Long currentUserId = userContextService.getCurrentUserId();
        Page<TodoEntity> todos = todoRepository.findByUserId(currentUserId, pageable);
        return todos.map(TodoResponse::from);
    }

    /**
     * ステータスでTODO一覧取得
     */
    public List<TodoResponse> getTodosByStatus(TodoStatus status) {
        log.debug("Getting TODOs with status: {}", status);
        
        Long currentUserId = userContextService.getCurrentUserId();
        List<TodoEntity> todos = todoRepository.findByUserIdAndStatus(currentUserId, status);
        return todos.stream()
            .map(TodoResponse::from)
            .toList();
    }

    /**
     * TODO更新
     */
    @Transactional
    public TodoResponse updateTodo(Long id, UpdateTodoRequest request) {
        log.debug("Updating TODO with id: {}", id);
        
        TodoEntity todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        
        Long currentUserId = userContextService.getCurrentUserId();
        if (!todo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to update TODO with id: " + id);
        }
            
        // Validate parent task if provided
        if (request.parentId() != null && !request.parentId().equals(todo.getParentId())) {
            TodoEntity parentTodo = todoRepository.findById(request.parentId())
                .orElseThrow(() -> new TodoNotFoundException(request.parentId()));
            
            // Ensure parent task belongs to the same user
            if (!parentTodo.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied to parent TODO with id: " + request.parentId());
            }
            
            // Prevent circular dependencies
            if (request.parentId().equals(id)) {
                throw new IllegalArgumentException("A task cannot be its own parent");
            }
        }
        
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setStatus(request.status());
        todo.setPriority(request.priority());
        todo.setDueDate(request.dueDate());
        todo.setParentId(request.parentId());
        
        TodoEntity updated = todoRepository.save(todo);
        log.info("Updated TODO with id: {} for user: {}", updated.getId(), currentUserId);
        
        return TodoResponse.from(updated);
    }

    /**
     * TODO削除
     */
    @Transactional
    public void deleteTodo(Long id) {
        log.debug("Deleting TODO with id: {}", id);
        
        TodoEntity todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        
        Long currentUserId = userContextService.getCurrentUserId();
        if (!todo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to delete TODO with id: " + id);
        }
        
        todoRepository.deleteById(id);
        log.info("Deleted TODO with id: {} for user: {}", id, currentUserId);
    }
    
    /**
     * Get child tasks for a parent task
     */
    public List<TodoResponse> getChildTasks(Long parentId) {
        log.debug("Getting child tasks for parent id: {}", parentId);
        
        // Verify parent task exists and user has access
        TodoEntity parentTodo = todoRepository.findById(parentId)
            .orElseThrow(() -> new TodoNotFoundException(parentId));
        
        Long currentUserId = userContextService.getCurrentUserId();
        if (!parentTodo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to TODO with id: " + parentId);
        }
        
        List<TodoEntity> childTasks = todoRepository.findByParentId(parentId);
        return childTasks.stream()
            .map(TodoResponse::from)
            .toList();
    }
}