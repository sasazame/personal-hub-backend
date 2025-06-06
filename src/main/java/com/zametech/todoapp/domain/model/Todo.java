package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Todo {
    
    private Long id;
    private String title;
    private String description;
    private TodoStatus status;
    private TodoPriority priority;
    private LocalDate dueDate;
    private Long userId;  // User ownership
    private Long parentId;  // Parent task ID for hierarchical structure
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    
    // Repeat fields
    private Boolean isRepeatable;
    private RepeatType repeatType;
    private Integer repeatInterval;
    private String repeatDaysOfWeek;  // Comma-separated string: "1,3,5"
    private Integer repeatDayOfMonth;
    private LocalDate repeatEndDate;
    private Long originalTodoId;  // Reference to original repeatable todo
}