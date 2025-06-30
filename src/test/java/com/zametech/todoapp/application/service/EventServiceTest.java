package com.zametech.todoapp.application.service;

import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.Event;
import com.zametech.todoapp.domain.repository.EventRepository;
import com.zametech.todoapp.presentation.dto.request.CreateEventRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private EventService eventService;

    private UUID userId;
    private Event event;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        startDateTime = LocalDateTime.now().plusDays(1);
        endDateTime = LocalDateTime.now().plusDays(1).plusHours(2);
        
        event = new Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(endDateTime);
        event.setLocation("Test Location");
        event.setAllDay(false);
        event.setReminderMinutes(30);
        event.setColor("#007bff");
        event.setUserId(userId);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createEvent_withValidRequest_shouldCreateAndReturnEvent() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
            "New Event",
            "Event Description",
            startDateTime,
            endDateTime,
            "Conference Room A",
            false,
            15,
            "#28a745"
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event savedEvent = invocation.getArgument(0);
            savedEvent.setId(2L);
            return savedEvent;
        });

        // When
        Event result = eventService.createEvent(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Event");
        assertThat(result.getDescription()).isEqualTo("Event Description");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStartDateTime()).isEqualTo(startDateTime);
        assertThat(result.getEndDateTime()).isEqualTo(endDateTime);
        assertThat(result.getLocation()).isEqualTo("Conference Room A");
        assertThat(result.isAllDay()).isFalse();
        assertThat(result.getReminderMinutes()).isEqualTo(15);
        assertThat(result.getColor()).isEqualTo("#28a745");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_withAllDayEvent_shouldSetAllDayTrue() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
            "All Day Event",
            "Full day conference",
            startDateTime,
            endDateTime,
            "Main Hall",
            true,
            60,
            "#ffc107"
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event savedEvent = invocation.getArgument(0);
            savedEvent.setId(3L);
            return savedEvent;
        });

        // When
        Event result = eventService.createEvent(request);

        // Then
        assertThat(result.isAllDay()).isTrue();
        assertThat(result.getTitle()).isEqualTo("All Day Event");
    }

    @Test
    void getEventById_withValidId_shouldReturnEvent() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(event));

        // When
        Event result = eventService.getEventById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getEventById_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventById(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Event not found with id: 999");
    }

    @Test
    void getEventById_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(eventRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventById(1L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Event not found with id: 1");
    }

    @Test
    void getEventsByUser_shouldReturnPagedEvents() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Event> eventList = Arrays.asList(event);
        Page<Event> eventPage = new PageImpl<>(eventList, pageRequest, 1);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByUserId(userId, pageRequest)).thenReturn(eventPage);

        // When
        Page<Event> result = eventService.getEventsByUser(pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getEventsByDateRange_shouldReturnFilteredEvents() {
        // Given
        LocalDateTime rangeStart = LocalDateTime.now();
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(7);
        
        Event event2 = new Event();
        event2.setId(2L);
        event2.setTitle("Event in Range");
        event2.setStartDateTime(rangeStart.plusDays(2));
        event2.setUserId(userId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByUserIdAndDateRange(userId, rangeStart, rangeEnd))
            .thenReturn(Arrays.asList(event, event2));

        // When
        List<Event> result = eventService.getEventsByDateRange(rangeStart, rangeEnd);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Event::getTitle)
            .containsExactly("Test Event", "Event in Range");
    }

    @Test
    void updateEvent_withFullUpdate_shouldUpdateAllFields() {
        // Given
        LocalDateTime newStartDateTime = LocalDateTime.now().plusDays(3);
        LocalDateTime newEndDateTime = LocalDateTime.now().plusDays(3).plusHours(4);
        
        UpdateEventRequest request = new UpdateEventRequest(
            "Updated Event",
            "Updated Description",
            newStartDateTime,
            newEndDateTime,
            "New Location",
            true,
            45,
            "#dc3545"
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Event result = eventService.updateEvent(1L, request);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Event");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getStartDateTime()).isEqualTo(newStartDateTime);
        assertThat(result.getEndDateTime()).isEqualTo(newEndDateTime);
        assertThat(result.getLocation()).isEqualTo("New Location");
        assertThat(result.isAllDay()).isTrue();
        assertThat(result.getReminderMinutes()).isEqualTo(45);
        assertThat(result.getColor()).isEqualTo("#dc3545");
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_withPartialUpdate_shouldUpdateOnlyProvidedFields() {
        // Given
        UpdateEventRequest request = new UpdateEventRequest(
            "Partially Updated Event",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Event result = eventService.updateEvent(1L, request);

        // Then
        assertThat(result.getTitle()).isEqualTo("Partially Updated Event");
        assertThat(result.getDescription()).isEqualTo("Test Description"); // Unchanged
        assertThat(result.getStartDateTime()).isEqualTo(startDateTime); // Unchanged
        assertThat(result.getLocation()).isEqualTo("Test Location"); // Unchanged
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        UpdateEventRequest request = new UpdateEventRequest(
            "Updated Event",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(999L, request))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Event not found with id: 999");
    }

    @Test
    void deleteEvent_withValidId_shouldDeleteEvent() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(event));

        // When
        eventService.deleteEvent(1L);

        // Then
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void deleteEvent_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(eventRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Event not found with id: 999");
    }

    @Test
    void deleteEvent_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(eventRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(1L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Event not found with id: 1");
    }
}