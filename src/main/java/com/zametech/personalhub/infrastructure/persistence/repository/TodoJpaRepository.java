package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TODO JpaRepository
 */
@Repository
public interface TodoJpaRepository extends JpaRepository<TodoEntity, Long> {
    
    /**
     * ステータスでTODOを検索する
     */
    List<TodoEntity> findByStatusOrderByCreatedAtDesc(TodoStatus status);
    
    /**
     * 優先度でTODOを検索する
     */
    List<TodoEntity> findByPriorityOrderByCreatedAtDesc(TodoPriority priority);
    
    /**
     * 期限が指定日以前のTODOを検索する
     */
    @Query("SELECT t FROM TodoEntity t WHERE t.dueDate <= :date AND t.status != :doneStatus ORDER BY t.dueDate ASC")
    List<TodoEntity> findByDueDateBeforeAndNotDone(@Param("date") LocalDate date, 
                                                    @Param("doneStatus") TodoStatus doneStatus);
    
    /**
     * タイトルに部分一致するTODOを検索する
     */
    List<TodoEntity> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
    
    /**
     * ユーザーIDでTODOを検索する
     */
    List<TodoEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * ユーザーIDとステータスでTODOを検索する
     */
    List<TodoEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TodoStatus status);
    
    /**
     * ユーザーIDでTODOを検索する（ページング）
     */
    Page<TodoEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * ユーザーIDですべてのTODOを削除する
     */
    void deleteByUserId(UUID userId);
    
    /**
     * 親タスクIDで子タスクを検索する
     */
    List<TodoEntity> findByParentIdOrderByCreatedAtDesc(Long parentId);
    
    /**
     * 繰り返し可能なTODOを検索する
     */
    List<TodoEntity> findByIsRepeatableTrueOrderByCreatedAtDesc();
    
    /**
     * ユーザーIDで繰り返し可能なTODOを検索する
     */
    List<TodoEntity> findByUserIdAndIsRepeatableTrueOrderByCreatedAtDesc(UUID userId);
    
    /**
     * 元TODOIDと期限日でTODOを検索する
     */
    List<TodoEntity> findByOriginalTodoIdAndDueDate(Long originalTodoId, LocalDate dueDate);
    
    /**
     * 元TODOIDで生成されたインスタンスを検索する
     */
    List<TodoEntity> findByOriginalTodoIdOrderByCreatedAtDesc(Long originalTodoId);
}