package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.PomodoroSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Pomodoro session operations.
 */
public interface PomodoroSessionRepository {
    PomodoroSession save(PomodoroSession session);
    Optional<PomodoroSession> findById(UUID id);
    List<PomodoroSession> findByUserId(UUID userId);
    Page<PomodoroSession> findByUserId(UUID userId, Pageable pageable);
    List<PomodoroSession> findByUserIdAndStatus(UUID userId, PomodoroSession.SessionStatus status);
    List<PomodoroSession> findByUserIdAndDateRange(UUID userId, LocalDateTime start, LocalDateTime end);
    void deleteById(UUID id);
    boolean existsById(UUID id);
    Optional<PomodoroSession> findActiveSessionByUserId(UUID userId);
}