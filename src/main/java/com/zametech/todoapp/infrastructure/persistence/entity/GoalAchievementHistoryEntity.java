package com.zametech.todoapp.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_achievement_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"goal_id", "achieved_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalAchievementHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "goal_id", nullable = false)
    private String goalId;
    
    @Column(name = "achieved_date", nullable = false)
    private LocalDate achievedDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}