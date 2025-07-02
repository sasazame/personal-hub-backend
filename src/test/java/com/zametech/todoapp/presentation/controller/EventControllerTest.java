package com.zametech.todoapp.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.application.service.EventService;
import com.zametech.todoapp.common.exception.TodoNotFoundException;
import com.zametech.todoapp.domain.model.Event;
import com.zametech.todoapp.presentation.dto.request.CreateEventRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateEventRequest;
import com.zametech.todoapp.presentation.dto.response.EventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = EventController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private Event sampleEvent;
    private CreateEventRequest createEventRequest;
    private UpdateEventRequest updateEventRequest;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event();
        sampleEvent.setId(1L);
        sampleEvent.setTitle("Test Event");
        sampleEvent.setDescription("Test Description");
        sampleEvent.setStartDateTime(LocalDateTime.now().plusDays(1));
        sampleEvent.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        sampleEvent.setLocation("Test Location");
        sampleEvent.setAllDay(false);
        sampleEvent.setReminderMinutes(15);
        sampleEvent.setColor("#0000FF");
        sampleEvent.setUserId(UUID.randomUUID());
        sampleEvent.setCreatedAt(LocalDateTime.now());
        sampleEvent.setUpdatedAt(LocalDateTime.now());

        createEventRequest = new CreateEventRequest(
            "New Event",
            "New Description",
            LocalDateTime.now().plusDays(2),
            LocalDateTime.now().plusDays(2).plusHours(1),
            "New Location",
            false,
            30, // reminderMinutes
            "#FF5733" // color
        );

        updateEventRequest = new UpdateEventRequest(
            "Updated Event",
            "Updated Description",
            LocalDateTime.now().plusDays(3),
            LocalDateTime.now().plusDays(3).plusHours(3),
            "Updated Location",
            false,
            60, // reminderMinutes
            "#0099CC" // color
        );
    }

    @Test
    @WithMockUser
    void createEvent_withValidRequest_shouldReturnCreatedEvent() throws Exception {
        when(eventService.createEvent(org.mockito.ArgumentMatchers.any(CreateEventRequest.class))).thenReturn(sampleEvent);

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createEventRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test Event"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.location").value("Test Location"))
            .andExpect(jsonPath("$.allDay").value(false));

        verify(eventService).createEvent(org.mockito.ArgumentMatchers.any(CreateEventRequest.class));
    }

    @Test
    @WithMockUser
    void createEvent_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateEventRequest invalidRequest = new CreateEventRequest(
            "", // Empty title
            null,
            null, // No start time
            null,
            null,
            false,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void getEvent_withValidId_shouldReturnEvent() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(sampleEvent);

        mockMvc.perform(get("/api/v1/events/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test Event"))
            .andExpect(jsonPath("$.description").value("Test Description"));

        verify(eventService).getEventById(1L);
    }

    @Test
    @WithMockUser
    void getEvent_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(eventService.getEventById(999L)).thenThrow(new TodoNotFoundException("Event not found with id: 999"));

        mockMvc.perform(get("/api/v1/events/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Event not found with id: 999"));

        verify(eventService).getEventById(999L);
    }

    @Test
    @WithMockUser
    void getEvent_withAccessDenied_shouldReturnForbidden() throws Exception {
        when(eventService.getEventById(1L)).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/events/1"))
            .andExpect(status().isForbidden());

        verify(eventService).getEventById(1L);
    }

    @Test
    @WithMockUser
    void getEvents_withPagination_shouldReturnPagedEvents() throws Exception {
        List<Event> eventList = Arrays.asList(sampleEvent);
        Page<Event> eventPage = new PageImpl<>(eventList, PageRequest.of(0, 10), 1);
        
        when(eventService.getEventsByUser(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(eventPage);

        mockMvc.perform(get("/api/v1/events")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "startDateTime,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0));

        verify(eventService).getEventsByUser(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getEvents_withEmptyResult_shouldReturnEmptyPage() throws Exception {
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        
        when(eventService.getEventsByUser(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(eventService).getEventsByUser(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getEventsByDateRange_withValidDates_shouldReturnFilteredEvents() throws Exception {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        List<Event> events = Arrays.asList(sampleEvent);
        
        when(eventService.getEventsByDateRange(org.mockito.ArgumentMatchers.any(LocalDateTime.class), 
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(events);

        mockMvc.perform(get("/api/v1/events/range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Test Event"));

        verify(eventService).getEventsByDateRange(org.mockito.ArgumentMatchers.any(LocalDateTime.class), 
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @WithMockUser
    void getEventsByDateRange_withNoEvents_shouldReturnEmptyList() throws Exception {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        
        when(eventService.getEventsByDateRange(org.mockito.ArgumentMatchers.any(LocalDateTime.class), 
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/events/range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(eventService).getEventsByDateRange(org.mockito.ArgumentMatchers.any(LocalDateTime.class), 
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @WithMockUser
    void updateEvent_withValidRequest_shouldReturnUpdatedEvent() throws Exception {
        Event updatedEvent = new Event();
        updatedEvent.setId(1L);
        updatedEvent.setTitle("Updated Event");
        updatedEvent.setDescription("Updated Description");
        updatedEvent.setStartDateTime(updateEventRequest.startDateTime());
        updatedEvent.setEndDateTime(updateEventRequest.endDateTime());
        updatedEvent.setLocation("Updated Location");
        updatedEvent.setAllDay(false);
        updatedEvent.setUserId(sampleEvent.getUserId());
        updatedEvent.setCreatedAt(sampleEvent.getCreatedAt());
        updatedEvent.setUpdatedAt(LocalDateTime.now());
        
        when(eventService.updateEvent(eq(1L), org.mockito.ArgumentMatchers.any(UpdateEventRequest.class))).thenReturn(updatedEvent);

        mockMvc.perform(put("/api/v1/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEventRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Updated Event"))
            .andExpect(jsonPath("$.description").value("Updated Description"))
            .andExpect(jsonPath("$.location").value("Updated Location"));

        verify(eventService).updateEvent(eq(1L), org.mockito.ArgumentMatchers.any(UpdateEventRequest.class));
    }

    @Test
    @WithMockUser
    void updateEvent_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(eventService.updateEvent(eq(999L), org.mockito.ArgumentMatchers.any(UpdateEventRequest.class)))
            .thenThrow(new TodoNotFoundException("Event not found with id: 999"));

        mockMvc.perform(put("/api/v1/events/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEventRequest)))
            .andExpect(status().isNotFound());

        verify(eventService).updateEvent(eq(999L), org.mockito.ArgumentMatchers.any(UpdateEventRequest.class));
    }

    // UpdateEventRequest has no validation constraints, so no bad request test needed

    @Test
    @WithMockUser
    void deleteEvent_withValidId_shouldReturnNoContent() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/v1/events/1"))
            .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(1L);
    }

    @Test
    @WithMockUser
    void deleteEvent_withNonExistentId_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException("Event not found with id: 999")).when(eventService).deleteEvent(999L);

        mockMvc.perform(delete("/api/v1/events/999"))
            .andExpect(status().isNotFound());

        verify(eventService).deleteEvent(999L);
    }

    @Test
    @WithMockUser
    void deleteEvent_withAccessDenied_shouldReturnForbidden() throws Exception {
        doThrow(new AccessDeniedException("Access denied")).when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/v1/events/1"))
            .andExpect(status().isForbidden());

        verify(eventService).deleteEvent(1L);
    }

    @Test
    @WithMockUser
    void createEvent_withAllDayEvent_shouldReturnCreatedEvent() throws Exception {
        CreateEventRequest allDayRequest = new CreateEventRequest(
            "All Day Event",
            "All day event description",
            LocalDateTime.now().plusDays(1).withHour(0).withMinute(0),
            LocalDateTime.now().plusDays(1).withHour(23).withMinute(59),
            "Conference Room",
            true,
            null, // no reminder for all day event
            "#00FF00" // color
        );

        Event allDayEvent = new Event();
        allDayEvent.setId(2L);
        allDayEvent.setTitle("All Day Event");
        allDayEvent.setDescription("All day event description");
        allDayEvent.setStartDateTime(allDayRequest.startDateTime());
        allDayEvent.setEndDateTime(allDayRequest.endDateTime());
        allDayEvent.setLocation("Conference Room");
        allDayEvent.setAllDay(true);
        allDayEvent.setUserId(UUID.randomUUID());
        allDayEvent.setCreatedAt(LocalDateTime.now());
        allDayEvent.setUpdatedAt(LocalDateTime.now());

        when(eventService.createEvent(org.mockito.ArgumentMatchers.any(CreateEventRequest.class))).thenReturn(allDayEvent);

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allDayRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.title").value("All Day Event"))
            .andExpect(jsonPath("$.allDay").value(true));

        verify(eventService).createEvent(org.mockito.ArgumentMatchers.any(CreateEventRequest.class));
    }
}