package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TODOリポジトリインターフェース
 */
public interface TodoRepository {
    
    /**
     * TODOを保存する
     */
    TodoEntity save(TodoEntity todo);
    
    /**
     * IDでTODOを検索する
     */
    Optional<TodoEntity> findById(Long id);
    
    /**
     * すべてのTODOを取得する（ページング）
     */
    Page<TodoEntity> findAll(Pageable pageable);
    
    /**
     * ステータスでTODOを検索する
     */
    List<TodoEntity> findByStatus(TodoStatus status);
    
    /**
     * 優先度でTODOを検索する
     */
    List<TodoEntity> findByPriority(TodoPriority priority);
    
    /**
     * 期限が指定日以前のTODOを検索する
     */
    List<TodoEntity> findByDueDateBefore(LocalDate date);
    
    /**
     * TODOを削除する
     */
    void deleteById(Long id);
    
    /**
     * IDでTODOが存在するか確認する
     */
    boolean existsById(Long id);
    
    /**
     * ユーザーIDでTODOを検索する（ページング）
     */
    Page<TodoEntity> findByUserId(Long userId, Pageable pageable);
    
    /**
     * ユーザーIDとステータスでTODOを検索する
     */
    List<TodoEntity> findByUserIdAndStatus(Long userId, TodoStatus status);
    
    /**
     * ユーザーIDですべてのTODOを削除する
     */
    void deleteByUserId(Long userId);
    
    /**
     * 親タスクIDで子タスクを検索する
     */
    List<TodoEntity> findByParentId(Long parentId);
    
    /**
     * 繰り返し可能なTODOを検索する
     */
    List<TodoEntity> findByIsRepeatableTrue();
    
    /**
     * ユーザーIDで繰り返し可能なTODOを検索する
     */
    List<TodoEntity> findByUserIdAndIsRepeatableTrue(Long userId);
    
    /**
     * 元TODOIDと期限日でTODOを検索する（繰り返しインスタンス検索用）
     */
    List<TodoEntity> findByOriginalTodoIdAndDueDate(Long originalTodoId, LocalDate dueDate);
    
    /**
     * 元TODOIDで生成されたインスタンスを検索する
     */
    List<TodoEntity> findByOriginalTodoId(Long originalTodoId);
}