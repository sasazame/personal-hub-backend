package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(Long id);
    Optional<Event> findByIdAndUserId(Long id, Long userId);
    Page<Event> findByUserId(Long userId, Pageable pageable);
    List<Event> findByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}