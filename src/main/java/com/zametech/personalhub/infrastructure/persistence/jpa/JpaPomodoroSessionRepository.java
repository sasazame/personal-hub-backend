package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Pomodoro session persistence.
 */
@Repository
public interface JpaPomodoroSessionRepository extends JpaRepository<PomodoroSessionEntity, UUID> {
    List<PomodoroSessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Page<PomodoroSessionEntity> findByUserId(UUID userId, Pageable pageable);
    
    List<PomodoroSessionEntity> findByUserIdAndStatus(UUID userId, PomodoroSessionEntity.SessionStatus status);
    
    @Query("SELECT p FROM PomodoroSessionEntity p WHERE p.userId = :userId AND p.createdAt BETWEEN :start AND :end ORDER BY p.createdAt DESC")
    List<PomodoroSessionEntity> findByUserIdAndDateRange(@Param("userId") UUID userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT p FROM PomodoroSessionEntity p WHERE p.userId = :userId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    Optional<PomodoroSessionEntity> findActiveSessionByUserId(@Param("userId") UUID userId);
}