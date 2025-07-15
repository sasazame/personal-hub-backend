package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.PomodoroConfig;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Pomodoro configuration operations.
 */
public interface PomodoroConfigRepository {
    PomodoroConfig save(PomodoroConfig config);
    Optional<PomodoroConfig> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}