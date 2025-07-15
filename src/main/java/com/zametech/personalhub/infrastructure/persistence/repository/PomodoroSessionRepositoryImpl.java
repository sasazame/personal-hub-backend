package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.PomodoroSession;
import com.zametech.personalhub.domain.model.PomodoroTask;
import com.zametech.personalhub.domain.repository.PomodoroSessionRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroSessionEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroTaskEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaPomodoroSessionRepository;
import com.zametech.personalhub.shared.constants.SessionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PomodoroSessionRepository using JPA.
 */
@Repository
@Transactional
public class PomodoroSessionRepositoryImpl implements PomodoroSessionRepository {
    private final JpaPomodoroSessionRepository jpaRepository;

    public PomodoroSessionRepositoryImpl(JpaPomodoroSessionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PomodoroSession save(PomodoroSession session) {
        PomodoroSessionEntity entity = toEntity(session);
        PomodoroSessionEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<PomodoroSession> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PomodoroSession> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PomodoroSession> findByUserId(UUID userId, Pageable pageable) {
        Page<PomodoroSessionEntity> entityPage = jpaRepository.findByUserId(userId, pageable);
        return entityPage.map(entity -> {
            // Initialize tasks to avoid lazy loading issues
            if (entity.getTasks() != null) {
                entity.getTasks().size(); // Force initialization
            }
            return toDomain(entity);
        });
    }

    @Override
    public List<PomodoroSession> findByUserIdAndStatus(UUID userId, PomodoroSession.SessionStatus status) {
        PomodoroSessionEntity.SessionStatus entityStatus = PomodoroSessionEntity.SessionStatus.valueOf(status.name());
        return jpaRepository.findByUserIdAndStatus(userId, entityStatus).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PomodoroSession> findByUserIdAndDateRange(UUID userId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByUserIdAndDateRange(userId, start, end).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public Optional<PomodoroSession> findActiveSessionByUserId(UUID userId) {
        return jpaRepository.findActiveSessionByUserId(userId).map(this::toDomain);
    }

    private PomodoroSession toDomain(PomodoroSessionEntity entity) {
        PomodoroSession session = new PomodoroSession();
        session.setId(entity.getId());
        session.setUserId(entity.getUserId());
        session.setStartTime(entity.getStartTime());
        session.setEndTime(entity.getEndTime());
        session.setWorkDuration(entity.getWorkDuration());
        session.setBreakDuration(entity.getBreakDuration());
        session.setCompletedCycles(entity.getCompletedCycles());
        session.setStatus(PomodoroSession.SessionStatus.valueOf(entity.getStatus().name()));
        session.setSessionType(entity.getSessionType());
        session.setCreatedAt(entity.getCreatedAt());
        session.setUpdatedAt(entity.getUpdatedAt());
        
        if (entity.getTasks() != null && !entity.getTasks().isEmpty()) {
            try {
                List<PomodoroTask> tasks = entity.getTasks().stream()
                        .map(this::taskToDomain)
                        .collect(Collectors.toList());
                session.setTasks(tasks);
            } catch (Exception e) {
                // Log error but don't fail the conversion
                session.setTasks(new ArrayList<>());
            }
        } else {
            session.setTasks(new ArrayList<>());
        }
        
        return session;
    }

    private PomodoroTask taskToDomain(PomodoroTaskEntity entity) {
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

    private PomodoroSessionEntity toEntity(PomodoroSession session) {
        PomodoroSessionEntity entity = new PomodoroSessionEntity();
        if (session.getId() != null) {
            entity.setId(session.getId());
        }
        entity.setUserId(session.getUserId());
        entity.setStartTime(session.getStartTime());
        entity.setEndTime(session.getEndTime());
        entity.setWorkDuration(session.getWorkDuration());
        entity.setBreakDuration(session.getBreakDuration());
        entity.setCompletedCycles(session.getCompletedCycles());
        entity.setStatus(PomodoroSessionEntity.SessionStatus.valueOf(session.getStatus().name()));
        entity.setSessionType(session.getSessionType());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setUpdatedAt(session.getUpdatedAt());
        
        return entity;
    }
}