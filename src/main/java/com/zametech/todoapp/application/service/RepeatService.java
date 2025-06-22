package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.RepeatType;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.repository.TodoRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.TodoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling repeatable todos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RepeatService {

    private final TodoRepository todoRepository;

    /**
     * Generate next occurrence of a repeatable todo
     */
    @Transactional
    public TodoEntity generateNextOccurrence(TodoEntity originalTodo) {
        if (!Boolean.TRUE.equals(originalTodo.getIsRepeatable()) || originalTodo.getRepeatType() == null) {
            throw new IllegalArgumentException("Todo is not repeatable");
        }

        LocalDate nextDueDate = calculateNextDueDate(originalTodo);
        
        if (nextDueDate == null || isRepeatEndReached(originalTodo, nextDueDate)) {
            log.debug("Repeat period ended for todo: {}", originalTodo.getId());
            return null;
        }
        
        // Check if instance already exists for this date
        List<TodoEntity> existingInstances = todoRepository.findByOriginalTodoIdAndDueDate(
            originalTodo.getId(), nextDueDate
        );
        if (!existingInstances.isEmpty()) {
            log.debug("Instance already exists for date {} for todo: {}", nextDueDate, originalTodo.getId());
            return null;
        }

        TodoEntity nextInstance = new TodoEntity(
                originalTodo.getUserId(),
                originalTodo.getTitle(),
                originalTodo.getDescription(),
                TodoStatus.TODO,
                originalTodo.getPriority(),
                nextDueDate,
                originalTodo.getParentId(),
                false, // Generated instances are not repeatable themselves
                null,
                null,
                null,
                null,
                null,
                originalTodo.getId() // Reference to original repeatable todo
        );

        TodoEntity saved = todoRepository.save(nextInstance);
        log.info("Generated next occurrence {} for repeatable todo {}", saved.getId(), originalTodo.getId());
        
        return saved;
    }

    /**
     * Calculate the next due date based on repeat configuration
     */
    public LocalDate calculateNextDueDate(TodoEntity todo) {
        if (todo.getDueDate() == null || todo.getRepeatType() == null) {
            return null;
        }

        LocalDate currentDueDate = todo.getDueDate();
        Integer interval = todo.getRepeatInterval() != null ? todo.getRepeatInterval() : 1;

        return switch (todo.getRepeatType()) {
            case DAILY -> currentDueDate.plusDays(interval);
            case WEEKLY -> calculateNextWeeklyDate(currentDueDate, todo.getRepeatDaysOfWeek());
            case MONTHLY -> calculateNextMonthlyDate(currentDueDate, todo.getRepeatDayOfMonth());
            case YEARLY -> currentDueDate.plusYears(interval);
            case ONCE -> null; // One-time occurrence, no next date
        };
    }

    /**
     * Calculate next weekly occurrence
     */
    private LocalDate calculateNextWeeklyDate(LocalDate currentDate, String daysOfWeekStr) {
        if (daysOfWeekStr == null || daysOfWeekStr.isEmpty()) {
            return currentDate.plusWeeks(1);
        }

        List<Integer> daysOfWeek = Arrays.stream(daysOfWeekStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());

        int currentDayOfWeek = currentDate.getDayOfWeek().getValue();
        
        // Find next occurrence within the same week
        for (Integer day : daysOfWeek) {
            if (day > currentDayOfWeek) {
                return currentDate.plusDays(day - currentDayOfWeek);
            }
        }
        
        // If no day found in current week, go to first day of next week
        Integer firstDay = daysOfWeek.get(0);
        long daysToAdd = 7 - currentDayOfWeek + firstDay;
        return currentDate.plusDays(daysToAdd);
    }

    /**
     * Calculate next monthly occurrence
     */
    private LocalDate calculateNextMonthlyDate(LocalDate currentDate, Integer dayOfMonth) {
        if (dayOfMonth == null) {
            return currentDate.plusMonths(1);
        }

        LocalDate nextMonth = currentDate.plusMonths(1);
        
        // Handle case where target day doesn't exist in next month (e.g., Feb 31st -> Feb 28th)
        int lastDayOfMonth = nextMonth.lengthOfMonth();
        int targetDay = Math.min(dayOfMonth, lastDayOfMonth);
        
        return nextMonth.withDayOfMonth(targetDay);
    }

    /**
     * Check if repeat end date has been reached
     */
    private boolean isRepeatEndReached(TodoEntity todo, LocalDate nextDate) {
        return todo.getRepeatEndDate() != null && nextDate.isAfter(todo.getRepeatEndDate());
    }

    /**
     * Find all repeatable todos that need next occurrences generated
     */
    public List<TodoEntity> findRepeatableTodosNeedingGeneration() {
        return todoRepository.findByIsRepeatableTrue()
                .stream()
                .filter(todo -> todo.getRepeatType() != RepeatType.ONCE)
                .filter(this::shouldGenerateNextOccurrence)
                .collect(Collectors.toList());
    }

    /**
     * Check if a repeatable todo should have its next occurrence generated
     */
    private boolean shouldGenerateNextOccurrence(TodoEntity todo) {
        if (todo.getDueDate() == null) {
            return false;
        }

        // Check if the current due date has passed or is today
        LocalDate today = LocalDate.now();
        if (todo.getDueDate().isAfter(today)) {
            return false;
        }

        // Check if next occurrence doesn't already exist
        LocalDate nextDueDate = calculateNextDueDate(todo);
        if (nextDueDate == null || isRepeatEndReached(todo, nextDueDate)) {
            return false;
        }

        // Check if an instance for the next due date already exists
        List<TodoEntity> existingInstances = todoRepository.findByOriginalTodoIdAndDueDate(
                todo.getId(), nextDueDate);
        
        return existingInstances.isEmpty();
    }

    /**
     * Generate all pending repeat occurrences for a user
     */
    @Transactional
    public List<TodoEntity> generateAllPendingOccurrences(UUID userId) {
        List<TodoEntity> repeatableTodos = todoRepository.findByUserIdAndIsRepeatableTrue(userId);
        
        return repeatableTodos.stream()
                .filter(this::shouldGenerateNextOccurrence)
                .map(this::generateNextOccurrence)
                .filter(todo -> todo != null)
                .collect(Collectors.toList());
    }

    /**
     * Parse days of week string to list
     */
    public List<Integer> parseDaysOfWeek(String daysOfWeekStr) {
        if (daysOfWeekStr == null || daysOfWeekStr.isEmpty()) {
            return List.of();
        }
        
        return Arrays.stream(daysOfWeekStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    /**
     * Format days of week list to string
     */
    public String formatDaysOfWeek(List<Integer> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }
        
        return daysOfWeek.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}