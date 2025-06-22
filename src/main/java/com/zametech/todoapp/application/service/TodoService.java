package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import com.zametech.todoapp.presentation.dto.request.CreateTodoRequest;
import com.zametech.todoapp.presentation.dto.request.RepeatConfigRequest;
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
    private final RepeatService repeatService;

    /**
     * TODO作成
     */
    @Transactional
    public TodoResponse createTodo(CreateTodoRequest request) {
        log.debug("Creating new TODO: {}", request.title());
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        
        // Validate parent task if provided
        if (request.parentId() != null) {
            TodoEntity parentTodo = todoRepository.findById(request.parentId())
                .orElseThrow(() -> new TodoNotFoundException(request.parentId()));
            
            // Ensure parent task belongs to the same user
            if (!parentTodo.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("Access denied to parent TODO with id: " + request.parentId());
            }
        }
        
        TodoEntity todo;
        
        if (Boolean.TRUE.equals(request.isRepeatable()) && request.repeatConfig() != null) {
            // 繰り返し設定ありのTODO作成
            todo = new TodoEntity(
                currentUserId,
                request.title(),
                request.description(),
                TodoStatus.TODO,
                request.priority(),
                request.dueDate(),
                request.parentId(),
                request.isRepeatable(),
                request.repeatConfig().repeatType(),
                request.repeatConfig().interval(),
                request.repeatConfig().getDaysOfWeekString(),
                request.repeatConfig().dayOfMonth(),
                request.repeatConfig().endDate(),
                null // originalTodoId - 元となるTODOなのでnull
            );
        } else {
            // 通常のTODO作成
            todo = new TodoEntity(
                currentUserId,
                request.title(),
                request.description(),
                TodoStatus.TODO,
                request.priority(),
                request.dueDate(),
                request.parentId()
            );
        }
        
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
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
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
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        Page<TodoEntity> todos = todoRepository.findByUserId(currentUserId, pageable);
        return todos.map(TodoResponse::from);
    }

    /**
     * ステータスでTODO一覧取得
     */
    public List<TodoResponse> getTodosByStatus(TodoStatus status) {
        log.debug("Getting TODOs with status: {}", status);
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
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
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
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
        
        // 基本フィールドの更新
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setStatus(request.status());
        todo.setPriority(request.priority());
        todo.setDueDate(request.dueDate());
        todo.setParentId(request.parentId());
        
        // 繰り返し設定の更新
        if (Boolean.TRUE.equals(request.isRepeatable()) && request.repeatConfig() != null) {
            todo.setIsRepeatable(true);
            todo.setRepeatType(request.repeatConfig().repeatType());
            todo.setRepeatInterval(request.repeatConfig().interval());
            todo.setRepeatDaysOfWeek(request.repeatConfig().getDaysOfWeekString());
            todo.setRepeatDayOfMonth(request.repeatConfig().dayOfMonth());
            todo.setRepeatEndDate(request.repeatConfig().endDate());
        } else {
            // 繰り返し設定を無効化
            todo.setIsRepeatable(false);
            todo.setRepeatType(null);
            todo.setRepeatInterval(null);
            todo.setRepeatDaysOfWeek(null);
            todo.setRepeatDayOfMonth(null);
            todo.setRepeatEndDate(null);
        }
        
        // TODOが完了状態になった場合、次の繰り返しインスタンスを生成
        if (request.status() == TodoStatus.DONE && Boolean.TRUE.equals(todo.getIsRepeatable()) && todo.getOriginalTodoId() == null) {
            handleTodoCompletion(todo);
        }
        
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
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
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
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        if (!parentTodo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to TODO with id: " + parentId);
        }
        
        List<TodoEntity> childTasks = todoRepository.findByParentId(parentId);
        return childTasks.stream()
            .map(TodoResponse::from)
            .toList();
    }
    
    /**
     * 繰り返し可能なTODO一覧取得
     */
    public List<TodoResponse> getRepeatableTodos() {
        log.debug("Getting all repeatable TODOs for current user");
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        List<TodoEntity> repeatableTodos = todoRepository.findByUserIdAndIsRepeatableTrue(currentUserId);
        return repeatableTodos.stream()
            .map(TodoResponse::from)
            .toList();
    }
    
    /**
     * 特定の繰り返しTODOから生成されたインスタンス一覧を取得
     */
    public List<TodoResponse> getRepeatInstances(Long originalTodoId) {
        log.debug("Getting repeat instances for original TODO id: {}", originalTodoId);
        
        // Verify original TODO exists and user has access
        TodoEntity originalTodo = todoRepository.findById(originalTodoId)
            .orElseThrow(() -> new TodoNotFoundException(originalTodoId));
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        if (!originalTodo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to TODO with id: " + originalTodoId);
        }
        
        List<TodoEntity> instances = todoRepository.findByOriginalTodoId(originalTodoId);
        return instances.stream()
            .map(TodoResponse::from)
            .toList();
    }
    
    /**
     * 期限到来した繰り返しTODOの新しいインスタンスを生成
     */
    @Transactional
    public List<TodoResponse> generatePendingRepeatInstances() {
        log.debug("Generating pending repeat instances for current user");
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        List<TodoEntity> newInstances = repeatService.generateAllPendingOccurrences(currentUserId);
        
        List<TodoEntity> savedInstances = newInstances.stream()
            .map(todoRepository::save)
            .toList();
        
        log.info("Generated {} new repeat instances for user: {}", savedInstances.size(), currentUserId);
        
        return savedInstances.stream()
            .map(TodoResponse::from)
            .toList();
    }
    
    /**
     * TODOステータス切り替え
     * TODO <-> DONE, IN_PROGRESS -> DONE に切り替え
     */
    @Transactional
    public TodoResponse toggleTodoStatus(Long id) {
        log.debug("Toggling TODO status for id: {}", id);
        
        TodoEntity todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        
        Long currentUserId = userContextService.getCurrentUserIdAsLong();
        if (!todo.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied to TODO with id: " + id);
        }
        
        // ステータス切り替えロジック
        TodoStatus newStatus = switch (todo.getStatus()) {
            case TODO -> TodoStatus.DONE;
            case IN_PROGRESS -> TodoStatus.DONE;
            case DONE -> TodoStatus.TODO;
        };
        
        todo.setStatus(newStatus);
        
        // 繰り返しタスクインスタンスが完了になった場合、新しいインスタンス生成
        if (newStatus == TodoStatus.DONE && todo.getOriginalTodoId() != null) {
            // このインスタンスのオリジナルTODOを取得
            TodoEntity originalTodo = todoRepository.findById(todo.getOriginalTodoId())
                .orElse(null);
            if (originalTodo != null && Boolean.TRUE.equals(originalTodo.getIsRepeatable())) {
                handleRepeatInstanceCompletion(originalTodo);
            }
        }
        
        TodoEntity updated = todoRepository.save(todo);
        log.info("Toggled TODO status from {} to {} for id: {} (user: {})", 
                 todo.getStatus(), newStatus, id, currentUserId);
        
        return TodoResponse.from(updated);
    }

    /**
     * TODO完了時の繰り返し処理
     */
    private void handleTodoCompletion(TodoEntity completedTodo) {
        if (!Boolean.TRUE.equals(completedTodo.getIsRepeatable()) || completedTodo.getOriginalTodoId() != null) {
            return; // 繰り返し設定がないか、すでに生成されたインスタンスの場合は何もしない
        }
        
        try {
            TodoEntity nextInstance = repeatService.generateNextOccurrence(completedTodo);
            if (nextInstance != null) {
                todoRepository.save(nextInstance);
                log.info("Generated next occurrence for completed TODO id: {}", completedTodo.getId());
            }
        } catch (Exception e) {
            log.error("Failed to generate next occurrence for TODO id: {}", completedTodo.getId(), e);
            // エラーをログに記録するが、元のTODOの更新は続行
        }
    }
    
    /**
     * 繰り返しタスクインスタンス完了時の処理
     */
    private void handleRepeatInstanceCompletion(TodoEntity originalTodo) {
        try {
            TodoEntity nextInstance = repeatService.generateNextOccurrence(originalTodo);
            if (nextInstance != null) {
                todoRepository.save(nextInstance);
                log.info("Generated next repeat instance for original TODO id: {}", originalTodo.getId());
            }
        } catch (Exception e) {
            log.error("Failed to generate next repeat instance for original TODO id: {}", originalTodo.getId(), e);
        }
    }
}