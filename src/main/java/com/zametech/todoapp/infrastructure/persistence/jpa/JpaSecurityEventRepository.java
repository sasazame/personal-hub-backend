package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {
    List<SecurityEvent> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);
    List<SecurityEvent> findByEventTypeAndCreatedAtAfter(SecurityEvent.EventType eventType, LocalDateTime after);
    
    @Query("SELECT COUNT(s) FROM SecurityEvent s WHERE s.user.id = :userId AND s.eventType = 'LOGIN_FAILURE' AND s.createdAt > :after")
    long countFailedLoginAttempts(UUID userId, LocalDateTime after);
}