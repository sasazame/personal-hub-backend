package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for Pomodoro task persistence.
 */
@Repository
public interface JpaPomodoroTaskRepository extends JpaRepository<PomodoroTaskEntity, UUID> {
    @Query("SELECT t FROM PomodoroTaskEntity t WHERE t.session.id = :sessionId ORDER BY t.orderIndex ASC")
    List<PomodoroTaskEntity> findBySessionId(@Param("sessionId") UUID sessionId);
    
    List<PomodoroTaskEntity> findByTodoId(Long todoId);
    
    @Modifying
    @Query("DELETE FROM PomodoroTaskEntity t WHERE t.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}