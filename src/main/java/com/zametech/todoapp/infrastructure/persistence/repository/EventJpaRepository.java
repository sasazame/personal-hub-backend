package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
    
    Optional<EventEntity> findByIdAndUserId(Long id, Long userId);
    
    Page<EventEntity> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT e FROM EventEntity e WHERE e.userId = :userId AND " +
           "((e.startDateTime >= :startDate AND e.startDateTime <= :endDate) OR " +
           "(e.endDateTime >= :startDate AND e.endDateTime <= :endDate) OR " +
           "(e.startDateTime <= :startDate AND e.endDateTime >= :endDate))")
    List<EventEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    void deleteByUserId(Long userId);
}