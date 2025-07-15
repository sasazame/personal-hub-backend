package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.PomodoroTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Pomodoro task operations.
 */
public interface PomodoroTaskRepository {
    PomodoroTask save(PomodoroTask task);
    Optional<PomodoroTask> findById(UUID id);
    List<PomodoroTask> findBySessionId(UUID sessionId);
    List<PomodoroTask> findByTodoId(Long todoId);
    void deleteById(UUID id);
    void deleteBySessionId(UUID sessionId);
    boolean existsById(UUID id);
}