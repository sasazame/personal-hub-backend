package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TODOリポジトリ実装
 */
@Repository
@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepository {

    private final TodoJpaRepository todoJpaRepository;

    @Override
    public TodoEntity save(TodoEntity todo) {
        return todoJpaRepository.save(todo);
    }

    @Override
    public Optional<TodoEntity> findById(Long id) {
        return todoJpaRepository.findById(id);
    }

    @Override
    public Page<TodoEntity> findAll(Pageable pageable) {
        return todoJpaRepository.findAll(pageable);
    }

    @Override
    public List<TodoEntity> findByStatus(TodoStatus status) {
        return todoJpaRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public List<TodoEntity> findByPriority(TodoPriority priority) {
        return todoJpaRepository.findByPriorityOrderByCreatedAtDesc(priority);
    }

    @Override
    public List<TodoEntity> findByDueDateBefore(LocalDate date) {
        return todoJpaRepository.findByDueDateBeforeAndNotDone(date, TodoStatus.DONE);
    }

    @Override
    public void deleteById(Long id) {
        todoJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return todoJpaRepository.existsById(id);
    }

    @Override
    public Page<TodoEntity> findByUserId(Long userId, Pageable pageable) {
        return todoJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public List<TodoEntity> findByUserIdAndStatus(Long userId, TodoStatus status) {
        return todoJpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }
    
    @Override
    public void deleteByUserId(Long userId) {
        todoJpaRepository.deleteByUserId(userId);
    }
    
    @Override
    public List<TodoEntity> findByParentId(Long parentId) {
        return todoJpaRepository.findByParentIdOrderByCreatedAtDesc(parentId);
    }
}