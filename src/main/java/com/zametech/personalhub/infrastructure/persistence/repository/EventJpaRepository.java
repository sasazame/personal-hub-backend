package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
    
    Optional<EventEntity> findByIdAndUserId(Long id, UUID userId);
    
    Page<EventEntity> findByUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT e FROM EventEntity e WHERE e.userId = :userId AND " +
           "((e.startDateTime >= :startDate AND e.startDateTime <= :endDate) OR " +
           "(e.endDateTime >= :startDate AND e.endDateTime <= :endDate) OR " +
           "(e.startDateTime <= :startDate AND e.endDateTime >= :endDate))")
    List<EventEntity> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    void deleteByUserId(UUID userId);
    
    // Google Calendar sync methods
    Optional<EventEntity> findByGoogleEventId(String googleEventId);
    
    List<EventEntity> findByUserIdAndSyncStatus(UUID userId, String syncStatus);
    
    List<EventEntity> findByUserIdAndLastSyncedAtAfter(UUID userId, LocalDateTime lastSyncedAt);
}