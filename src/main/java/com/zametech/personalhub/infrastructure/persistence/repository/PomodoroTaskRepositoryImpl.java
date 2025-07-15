package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.PomodoroTask;
import com.zametech.personalhub.domain.repository.PomodoroTaskRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroSessionEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroTaskEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaPomodoroSessionRepository;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaPomodoroTaskRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PomodoroTaskRepository using JPA.
 */
@Repository
@Transactional
public class PomodoroTaskRepositoryImpl implements PomodoroTaskRepository {
    private final JpaPomodoroTaskRepository jpaRepository;
    private final JpaPomodoroSessionRepository sessionRepository;

    public PomodoroTaskRepositoryImpl(JpaPomodoroTaskRepository jpaRepository,
                                      JpaPomodoroSessionRepository sessionRepository) {
        this.jpaRepository = jpaRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public PomodoroTask save(PomodoroTask task) {
        PomodoroTaskEntity entity = toEntity(task);
        PomodoroTaskEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<PomodoroTask> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PomodoroTask> findBySessionId(UUID sessionId) {
        return jpaRepository.findBySessionId(sessionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PomodoroTask> findByTodoId(Long todoId) {
        return jpaRepository.findByTodoId(todoId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        jpaRepository.deleteBySessionId(sessionId);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    private PomodoroTask toDomain(PomodoroTaskEntity entity) {
        PomodoroTask task = new PomodoroTask();
        task.setId(entity.getId());
        task.setSessionId(entity.getSession().getId());
        task.setTodoId(entity.getTodoId());
        task.setDescription(entity.getDescription());
        task.setCompleted(entity.getCompleted());
        task.setOrderIndex(entity.getOrderIndex());
        task.setCreatedAt(entity.getCreatedAt());
        task.setUpdatedAt(entity.getUpdatedAt());
        return task;
    }

    private PomodoroTaskEntity toEntity(PomodoroTask task) {
        PomodoroTaskEntity entity = new PomodoroTaskEntity();
        if (task.getId() != null) {
            entity.setId(task.getId());
        }
        
        // Set the session
        PomodoroSessionEntity session = sessionRepository.findById(task.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + task.getSessionId()));
        entity.setSession(session);
        
        entity.setTodoId(task.getTodoId());
        entity.setDescription(task.getDescription());
        entity.setCompleted(task.getCompleted());
        entity.setOrderIndex(task.getOrderIndex());
        entity.setCreatedAt(task.getCreatedAt());
        entity.setUpdatedAt(task.getUpdatedAt());
        
        return entity;
    }
}