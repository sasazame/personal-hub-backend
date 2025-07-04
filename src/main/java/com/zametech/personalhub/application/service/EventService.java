package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Event;
import com.zametech.personalhub.domain.repository.EventRepository;
import com.zametech.personalhub.presentation.dto.request.CreateEventRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserContextService userContextService;

    @Transactional
    public Event createEvent(CreateEventRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDateTime(request.startDateTime());
        event.setEndDateTime(request.endDateTime());
        event.setLocation(request.location());
        event.setAllDay(request.allDay());
        event.setReminderMinutes(request.reminderMinutes());
        event.setColor(request.color());
        event.setUserId(currentUserId);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        log.info("Creating new event: {} for user: {}", request.title(), currentUserId);
        return eventRepository.save(event);
    }

    public Event getEventById(Long eventId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Event event = eventRepository.findByIdAndUserId(eventId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Event not found with id: " + eventId));
        
        log.info("Getting event with id: {} for user: {}", eventId, currentUserId);
        return event;
    }

    public Page<Event> getEventsByUser(Pageable pageable) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting events for user: {} with pageable: {}", currentUserId, pageable);
        return eventRepository.findByUserId(currentUserId, pageable);
    }

    public List<Event> getEventsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting events for user: {} between {} and {}", currentUserId, startDate, endDate);
        return eventRepository.findByUserIdAndDateRange(currentUserId, startDate, endDate);
    }

    @Transactional
    public Event updateEvent(Long eventId, UpdateEventRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Event event = eventRepository.findByIdAndUserId(eventId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Event not found with id: " + eventId));

        if (request.title() != null) {
            event.setTitle(request.title());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if (request.startDateTime() != null) {
            event.setStartDateTime(request.startDateTime());
        }
        if (request.endDateTime() != null) {
            event.setEndDateTime(request.endDateTime());
        }
        if (request.location() != null) {
            event.setLocation(request.location());
        }
        if (request.allDay() != null) {
            event.setAllDay(request.allDay());
        }
        if (request.reminderMinutes() != null) {
            event.setReminderMinutes(request.reminderMinutes());
        }
        if (request.color() != null) {
            event.setColor(request.color());
        }

        log.info("Updating event with id: {} for user: {}", eventId, currentUserId);
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Event event = eventRepository.findByIdAndUserId(eventId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Event not found with id: " + eventId));

        log.info("Deleting event with id: {} for user: {}", eventId, currentUserId);
        eventRepository.deleteById(eventId);
    }
}