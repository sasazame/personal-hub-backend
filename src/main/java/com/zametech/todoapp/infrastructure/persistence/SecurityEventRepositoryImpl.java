package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.SecurityEvent;
import com.zametech.todoapp.domain.repository.SecurityEventRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaSecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SecurityEventRepositoryImpl implements SecurityEventRepository {
    
    private final JpaSecurityEventRepository jpaRepository;
    
    @Override
    public SecurityEvent save(SecurityEvent event) {
        return jpaRepository.save(event);
    }
    
    @Override
    public List<SecurityEvent> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after) {
        return jpaRepository.findByUserIdAndCreatedAtAfter(userId, after);
    }
    
    @Override
    public List<SecurityEvent> findByEventTypeAndCreatedAtAfter(SecurityEvent.EventType eventType, LocalDateTime after) {
        return jpaRepository.findByEventTypeAndCreatedAtAfter(eventType, after);
    }
    
    @Override
    public long countFailedLoginAttempts(UUID userId, LocalDateTime after) {
        return jpaRepository.countFailedLoginAttempts(userId, after);
    }
}