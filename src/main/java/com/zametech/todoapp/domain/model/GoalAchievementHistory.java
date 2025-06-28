package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalAchievementHistory {
    private String id;
    private String goalId;
    private LocalDate achievedDate;
    private LocalDateTime createdAt;
}