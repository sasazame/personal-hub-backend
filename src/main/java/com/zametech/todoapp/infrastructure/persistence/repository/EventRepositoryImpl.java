package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.Event;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.EventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository eventJpaRepository;

    @Override
    public Event save(Event event) {
        EventEntity entity = toEntity(event);
        EventEntity savedEntity = eventJpaRepository.save(entity);
        return toModel(savedEntity);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventJpaRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    public Optional<Event> findByIdAndUserId(Long id, Long userId) {
        return eventJpaRepository.findByIdAndUserId(id, userId)
                .map(this::toModel);
    }

    @Override
    public Page<Event> findByUserId(Long userId, Pageable pageable) {
        return eventJpaRepository.findByUserId(userId, pageable)
                .map(this::toModel);
    }

    @Override
    public List<Event> findByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return eventJpaRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        eventJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        eventJpaRepository.deleteByUserId(userId);
    }

    private EventEntity toEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setId(event.getId());
        entity.setTitle(event.getTitle());
        entity.setDescription(event.getDescription());
        entity.setStartDateTime(event.getStartDateTime());
        entity.setEndDateTime(event.getEndDateTime());
        entity.setLocation(event.getLocation());
        entity.setAllDay(event.isAllDay());
        entity.setReminderMinutes(event.getReminderMinutes());
        entity.setColor(event.getColor());
        entity.setUserId(event.getUserId());
        entity.setCreatedAt(event.getCreatedAt());
        entity.setUpdatedAt(event.getUpdatedAt());
        return entity;
    }

    private Event toModel(EventEntity entity) {
        return new Event(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartDateTime(),
                entity.getEndDateTime(),
                entity.getLocation(),
                entity.isAllDay(),
                entity.getReminderMinutes(),
                entity.getColor(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}