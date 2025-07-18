package com.zametech.personalhub.application.service;

import com.zametech.personalhub.domain.model.Event;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.domain.repository.EventRepository;
import com.zametech.personalhub.domain.repository.NoteRepository;
import com.zametech.personalhub.domain.repository.TodoRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;
import com.zametech.personalhub.presentation.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TodoRepository todoRepository;
    private final EventRepository eventRepository;
    private final NoteRepository noteRepository;
    private final UserContextService userContextService;

    public DashboardResponse getDashboard() {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Generating dashboard for user: {}", currentUserId);
        
        TodoStatsResponse todoStats = generateTodoStats(currentUserId);
        EventStatsResponse eventStats = generateEventStats(currentUserId);
        NoteStatsResponse noteStats = generateNoteStats(currentUserId);
        ProductivityStatsResponse productivityStats = generateProductivityStats(currentUserId);
        
        return new DashboardResponse(todoStats, eventStats, noteStats, productivityStats);
    }

    public TodoActivityResponse getTodoActivity(LocalDate startDate, LocalDate endDate) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Generating TODO activity for user: {} from {} to {}", currentUserId, startDate, endDate);
        
        List<TodoEntity> allTodos = todoRepository.findByUserId(currentUserId, PageRequest.of(0, 1000)).getContent();
        
        // Daily completions for the specified date range
        Map<LocalDate, Long> dailyCompletions = generateDailyCompletionsInRange(allTodos, startDate, endDate);
        
        // Daily creations for the specified date range
        Map<LocalDate, Long> dailyCreations = generateDailyCreationsInRange(allTodos, startDate, endDate);
        
        // Priority distribution (all todos)
        Map<String, Integer> priorityDistribution = allTodos.stream()
                .collect(Collectors.groupingBy(
                        todo -> todo.getPriority().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        
        // Status distribution (all todos)
        Map<String, Integer> statusDistribution = allTodos.stream()
                .collect(Collectors.groupingBy(
                        todo -> todo.getStatus().name(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        
        // Average completion time
        double averageCompletionTime = calculateAverageCompletionTime(allTodos);
        
        // Convert Maps to Lists of DailyCount
        List<DailyCount> dailyCompletionsList = convertMapToList(dailyCompletions);
        List<DailyCount> dailyCreationsList = convertMapToList(dailyCreations);
        
        return new TodoActivityResponse(
                dailyCompletionsList,
                dailyCreationsList,
                priorityDistribution,
                statusDistribution,
                averageCompletionTime
        );
    }

    private TodoStatsResponse generateTodoStats(UUID userId) {
        List<TodoEntity> allTodos = todoRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        
        long totalTodos = allTodos.size();
        long completedTodos = allTodos.stream()
                .mapToLong(todo -> todo.getStatus() == TodoStatus.DONE ? 1 : 0)
                .sum();
        long inProgressTodos = allTodos.stream()
                .mapToLong(todo -> todo.getStatus() == TodoStatus.IN_PROGRESS ? 1 : 0)
                .sum();
        long pendingTodos = allTodos.stream()
                .mapToLong(todo -> todo.getStatus() == TodoStatus.TODO ? 1 : 0)
                .sum();
        
        double completionRate = totalTodos > 0 ? (double) completedTodos / totalTodos * 100 : 0.0;
        
        long overdueCount = allTodos.stream()
                .mapToLong(todo -> isOverdue(todo) ? 1 : 0)
                .sum();
        
        return new TodoStatsResponse(
                totalTodos,
                completedTodos,
                inProgressTodos,
                pendingTodos,
                Math.round(completionRate * 100.0) / 100.0,
                overdueCount
        );
    }

    private EventStatsResponse generateEventStats(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        
        List<Event> allEvents = eventRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        
        long totalEvents = allEvents.size();
        long upcomingEvents = allEvents.stream()
                .mapToLong(event -> event.getStartDateTime().isAfter(now) ? 1 : 0)
                .sum();
        long pastEvents = allEvents.stream()
                .mapToLong(event -> event.getEndDateTime().isBefore(now) ? 1 : 0)
                .sum();
        long todayEvents = allEvents.stream()
                .mapToLong(event -> isEventToday(event, startOfDay, endOfDay) ? 1 : 0)
                .sum();
        
        return new EventStatsResponse(totalEvents, upcomingEvents, pastEvents, todayEvents);
    }

    private NoteStatsResponse generateNoteStats(UUID userId) {
        List<Note> allNotes = noteRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        
        long totalNotes = allNotes.size();
        long notesThisWeek = allNotes.stream()
                .mapToLong(note -> note.getCreatedAt().isAfter(weekAgo) ? 1 : 0)
                .sum();
        long notesThisMonth = allNotes.stream()
                .mapToLong(note -> note.getCreatedAt().isAfter(monthAgo) ? 1 : 0)
                .sum();
        
        Set<String> uniqueTags = allNotes.stream()
                .flatMap(note -> note.getTagList().stream())
                .collect(Collectors.toSet());
        
        return new NoteStatsResponse(totalNotes, notesThisWeek, notesThisMonth, uniqueTags.size());
    }

    private ProductivityStatsResponse generateProductivityStats(UUID userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        
        List<TodoEntity> todos = todoRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<Event> events = eventRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<Note> notes = noteRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        
        Map<LocalDate, Long> dailyTodoCompletions = generateDailyCompletionsInRange(todos, startDate, endDate);
        Map<LocalDate, Long> dailyEventCounts = generateDailyEventCountsInRange(events, startDate, endDate);
        Map<LocalDate, Long> dailyNoteCreations = generateDailyNoteCreationsInRange(notes, startDate, endDate);
        
        double weeklyProductivityScore = calculateWeeklyProductivityScore(
                dailyTodoCompletions, dailyEventCounts, dailyNoteCreations);
        
        // Convert Maps to Lists of DailyCount
        List<DailyCount> dailyTodoCompletionsList = convertMapToList(dailyTodoCompletions);
        List<DailyCount> dailyEventCountsList = convertMapToList(dailyEventCounts);
        List<DailyCount> dailyNoteCreationsList = convertMapToList(dailyNoteCreations);
        
        return new ProductivityStatsResponse(
                dailyTodoCompletionsList,
                dailyEventCountsList,
                dailyNoteCreationsList,
                Math.round(weeklyProductivityScore * 100.0) / 100.0
        );
    }

    private Map<LocalDate, Long> generateDailyCompletions(List<TodoEntity> todos) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return generateDailyCompletionsInRange(todos, startDate, endDate);
    }

    private Map<LocalDate, Long> generateDailyCompletionsInRange(List<TodoEntity> todos, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyCompletions = new LinkedHashMap<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCompletions.put(date, 0L);
        }
        
        todos.stream()
                .filter(todo -> todo.getStatus() == TodoStatus.DONE && todo.getUpdatedAt() != null)
                .forEach(todo -> {
                    LocalDate completionDate = todo.getUpdatedAt().toLocalDate();
                    if (!completionDate.isBefore(startDate) && !completionDate.isAfter(endDate)) {
                        dailyCompletions.merge(completionDate, 1L, Long::sum);
                    }
                });
        
        return dailyCompletions;
    }

    private Map<LocalDate, Long> generateDailyCreations(List<TodoEntity> todos) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        return generateDailyCreationsInRange(todos, startDate, endDate);
    }

    private Map<LocalDate, Long> generateDailyCreationsInRange(List<TodoEntity> todos, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyCreations = new LinkedHashMap<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyCreations.put(date, 0L);
        }
        
        todos.stream()
                .filter(todo -> todo.getCreatedAt() != null)
                .forEach(todo -> {
                    LocalDate creationDate = todo.getCreatedAt().toLocalDate();
                    if (!creationDate.isBefore(startDate) && !creationDate.isAfter(endDate)) {
                        dailyCreations.merge(creationDate, 1L, Long::sum);
                    }
                });
        
        return dailyCreations;
    }

    private Map<LocalDate, Long> generateDailyEventCountsInRange(List<Event> events, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyEventCounts = new LinkedHashMap<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyEventCounts.put(date, 0L);
        }
        
        events.forEach(event -> {
            LocalDate eventDate = event.getStartDateTime().toLocalDate();
            if (!eventDate.isBefore(startDate) && !eventDate.isAfter(endDate)) {
                dailyEventCounts.merge(eventDate, 1L, Long::sum);
            }
        });
        
        return dailyEventCounts;
    }

    private Map<LocalDate, Long> generateDailyNoteCreationsInRange(List<Note> notes, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyNoteCreations = new LinkedHashMap<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyNoteCreations.put(date, 0L);
        }
        
        notes.forEach(note -> {
            LocalDate creationDate = note.getCreatedAt().toLocalDate();
            if (!creationDate.isBefore(startDate) && !creationDate.isAfter(endDate)) {
                dailyNoteCreations.merge(creationDate, 1L, Long::sum);
            }
        });
        
        return dailyNoteCreations;
    }

    private double calculateAverageCompletionTime(List<TodoEntity> todos) {
        List<Long> completionTimes = todos.stream()
                .filter(todo -> todo.getStatus() == TodoStatus.DONE)
                .filter(todo -> todo.getCreatedAt() != null && todo.getUpdatedAt() != null)
                .map(todo -> ChronoUnit.DAYS.between(todo.getCreatedAt().toLocalDate(), todo.getUpdatedAt().toLocalDate()))
                .filter(days -> days >= 0)
                .toList();
        
        return completionTimes.isEmpty() ? 0.0 : completionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    private double calculateWeeklyProductivityScore(
            Map<LocalDate, Long> todoCompletions,
            Map<LocalDate, Long> eventCounts,
            Map<LocalDate, Long> noteCreations) {
        
        long totalTodoCompletions = todoCompletions.values().stream().mapToLong(Long::longValue).sum();
        long totalEvents = eventCounts.values().stream().mapToLong(Long::longValue).sum();
        long totalNotes = noteCreations.values().stream().mapToLong(Long::longValue).sum();
        
        // Simple scoring algorithm: weighted sum of activities
        return (totalTodoCompletions * 3.0) + (totalEvents * 1.0) + (totalNotes * 2.0);
    }

    private boolean isOverdue(TodoEntity todo) {
        return todo.getDueDate() != null && 
               todo.getDueDate().isBefore(LocalDate.now()) && 
               todo.getStatus() != TodoStatus.DONE;
    }

    private boolean isEventToday(Event event, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return (event.getStartDateTime().isBefore(endOfDay) && event.getEndDateTime().isAfter(startOfDay));
    }

    private List<DailyCount> convertMapToList(Map<LocalDate, Long> dailyData) {
        return dailyData.entrySet().stream()
                .map(entry -> new DailyCount(entry.getKey(), Math.toIntExact(entry.getValue())))
                .collect(Collectors.toList());
    }
}