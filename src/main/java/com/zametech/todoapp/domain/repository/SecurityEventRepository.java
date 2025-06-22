package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.SecurityEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SecurityEventRepository {
    SecurityEvent save(SecurityEvent event);
    List<SecurityEvent> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);
    List<SecurityEvent> findByEventTypeAndCreatedAtAfter(SecurityEvent.EventType eventType, LocalDateTime after);
    long countFailedLoginAttempts(UUID userId, LocalDateTime after);
    List<SecurityEvent> findRecentEventsByUser(UUID userId, int limit);
    long countByEventTypeAndSuccessAndCreatedAtAfter(SecurityEvent.EventType eventType, boolean success, LocalDateTime after);
}