package com.zametech.personalhub.application.service;

import com.zametech.personalhub.domain.model.Event;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.domain.repository.EventRepository;
import com.zametech.personalhub.domain.repository.NoteRepository;
import com.zametech.personalhub.domain.repository.TodoRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;
import com.zametech.personalhub.presentation.dto.response.DashboardResponse;
import com.zametech.personalhub.presentation.dto.response.TodoActivityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID testUserId;
    private List<TodoEntity> testTodos;
    private List<Event> testEvents;
    private List<Note> testNotes;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Setup test todos
        testTodos = Arrays.asList(
            createTodoEntity(1L, "Todo 1", TodoStatus.DONE, TodoPriority.HIGH, LocalDate.now().minusDays(1)),
            createTodoEntity(2L, "Todo 2", TodoStatus.IN_PROGRESS, TodoPriority.MEDIUM, null),
            createTodoEntity(3L, "Todo 3", TodoStatus.TODO, TodoPriority.LOW, LocalDate.now().minusDays(1)) // Past due date to create overdue todo
        );
        
        // Setup test events
        testEvents = Arrays.asList(
            createEvent(1L, "Event 1", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)),
            createEvent(2L, "Event 2", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1))
        );
        
        // Setup test notes
        testNotes = Arrays.asList(
            createNote(1L, "Note 1", LocalDateTime.now().minusDays(1)),
            createNote(2L, "Note 2", LocalDateTime.now().minusDays(7))
        );
    }

    @Test
    void testGetDashboard() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(testUserId);
        when(todoRepository.findByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testTodos));
        when(eventRepository.findByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testEvents));
        when(noteRepository.findByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testNotes));

        // When
        DashboardResponse result = analyticsService.getDashboard();

        // Then
        assertNotNull(result);
        assertNotNull(result.todoStats());
        assertNotNull(result.eventStats());
        assertNotNull(result.noteStats());
        assertNotNull(result.productivityStats());
        
        assertEquals(3, result.todoStats().totalTodos());
        assertEquals(1, result.todoStats().completedTodos());
        assertEquals(1, result.todoStats().inProgressTodos());
        assertEquals(1, result.todoStats().pendingTodos());
        assertEquals(1, result.todoStats().overdueCount());
        
        assertEquals(2, result.eventStats().totalEvents());
        assertEquals(2, result.noteStats().totalNotes());
    }

    @Test
    void testGetTodoActivity() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(userContextService.getCurrentUserId()).thenReturn(testUserId);
        when(todoRepository.findByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testTodos));

        // When
        TodoActivityResponse result = analyticsService.getTodoActivity(startDate, endDate);

        // Then
        assertNotNull(result);
        assertNotNull(result.dailyCompletions());
        assertNotNull(result.dailyCreations());
        assertNotNull(result.priorityDistribution());
        assertNotNull(result.statusDistribution());
        
        assertEquals(3, result.priorityDistribution().get("HIGH") + 
                        result.priorityDistribution().get("MEDIUM") + 
                        result.priorityDistribution().get("LOW"));
        assertEquals(3, result.statusDistribution().get("DONE") + 
                        result.statusDistribution().get("IN_PROGRESS") + 
                        result.statusDistribution().get("TODO"));
    }

    private TodoEntity createTodoEntity(Long id, String title, TodoStatus status, TodoPriority priority, LocalDate dueDate) {
        TodoEntity todo = new TodoEntity();
        todo.setId(id);
        todo.setTitle(title);
        todo.setStatus(status);
        todo.setPriority(priority);
        todo.setDueDate(dueDate);
        todo.setCreatedAt(ZonedDateTime.now().minusDays(5));
        if (status == TodoStatus.DONE) {
            todo.setUpdatedAt(ZonedDateTime.now().minusDays(1));
        }
        return todo;
    }

    private Event createEvent(Long id, String title, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(endDateTime);
        event.setUserId(testUserId);
        return event;
    }

    private Note createNote(Long id, String title, LocalDateTime createdAt) {
        Note note = new Note();
        note.setId(id);
        note.setTitle(title);
        note.setContent("Test content");
        note.setTags("");
        note.setUserId(testUserId);
        note.setCreatedAt(createdAt);
        return note;
    }
}