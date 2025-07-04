package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(Long id);
    Optional<Event> findByIdAndUserId(Long id, UUID userId);
    Page<Event> findByUserId(UUID userId, Pageable pageable);
    List<Event> findByUserIdAndDateRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate);
    void deleteById(Long id);
    void deleteByUserId(UUID userId);
    
    // Google Calendar sync methods
    Optional<Event> findByGoogleEventId(String googleEventId);
    List<Event> findByUserIdAndSyncStatus(UUID userId, String syncStatus);
    List<Event> findByUserIdAndLastSyncedAtAfter(UUID userId, LocalDateTime lastSyncedAt);
}