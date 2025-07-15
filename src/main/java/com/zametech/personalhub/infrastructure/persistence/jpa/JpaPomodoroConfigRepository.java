package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Pomodoro configuration persistence.
 */
@Repository
public interface JpaPomodoroConfigRepository extends JpaRepository<PomodoroConfigEntity, UUID> {
    Optional<PomodoroConfigEntity> findByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
    
    boolean existsByUserId(UUID userId);
}