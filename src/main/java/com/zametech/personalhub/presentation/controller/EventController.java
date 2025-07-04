package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.EventService;
import com.zametech.personalhub.domain.model.Event;
import com.zametech.personalhub.presentation.dto.request.CreateEventRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateEventRequest;
import com.zametech.personalhub.presentation.dto.response.EventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        log.info("Creating event: {}", request.title());
        Event event = eventService.createEvent(request);
        EventResponse response = mapToEventResponse(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        log.info("Getting event with id: {}", id);
        Event event = eventService.getEventById(id);
        EventResponse response = mapToEventResponse(event);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(Pageable pageable) {
        log.info("Getting events with pageable: {}", pageable);
        Page<Event> events = eventService.getEventsByUser(pageable);
        Page<EventResponse> response = events.map(this::mapToEventResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/range")
    public ResponseEntity<List<EventResponse>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting events between {} and {}", startDate, endDate);
        List<Event> events = eventService.getEventsByDateRange(startDate, endDate);
        List<EventResponse> response = events.stream()
                .map(this::mapToEventResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        log.info("Updating event with id: {}", id);
        Event event = eventService.updateEvent(id, request);
        EventResponse response = mapToEventResponse(event);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        log.info("Deleting event with id: {}", id);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    private EventResponse mapToEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getLocation(),
                event.isAllDay(),
                event.getReminderMinutes(),
                event.getColor(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}