package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.domain.model.SecurityEvent;
import com.zametech.personalhub.infrastructure.persistence.entity.SecurityEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSecurityEventRepository extends JpaRepository<SecurityEventEntity, UUID> {
    List<SecurityEventEntity> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);
    List<SecurityEventEntity> findByEventTypeAndCreatedAtAfter(SecurityEvent.EventType eventType, LocalDateTime after);
    
    @Query("SELECT COUNT(s) FROM SecurityEventEntity s WHERE s.user.id = :userId AND s.eventType = 'LOGIN_FAILURE' AND s.createdAt > :after")
    long countFailedLoginAttempts(@Param("userId") UUID userId, @Param("after") LocalDateTime after);
    
    @Query("SELECT s FROM SecurityEventEntity s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<SecurityEventEntity> findTopByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
    
    default List<SecurityEventEntity> findTopByUserIdOrderByCreatedAtDesc(UUID userId, int limit) {
        return findTopByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(limit));
    }
    
    long countByEventTypeAndSuccessAndCreatedAtAfter(SecurityEvent.EventType eventType, boolean success, LocalDateTime after);
}