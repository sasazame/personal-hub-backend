package com.zametech.todoapp.infrastructure.persistence.entity;

import com.zametech.todoapp.domain.model.TodoPriority;
import com.zametech.todoapp.domain.model.TodoStatus;
import com.zametech.todoapp.domain.model.RepeatType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * TODOエンティティ
 */
@Entity
@Table(name = "todos")
public class TodoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status = TodoStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TodoPriority priority = TodoPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private TodoEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TodoEntity> children = new java.util.ArrayList<>();

    // Repeat fields
    @Column(name = "is_repeatable")
    private Boolean isRepeatable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", length = 50)
    private RepeatType repeatType;

    @Column(name = "repeat_interval")
    private Integer repeatInterval;

    @Column(name = "repeat_days_of_week", length = 20)
    private String repeatDaysOfWeek;

    @Column(name = "repeat_day_of_month")
    private Integer repeatDayOfMonth;

    @Column(name = "repeat_end_date")
    private LocalDate repeatEndDate;

    @Column(name = "original_todo_id")
    private Long originalTodoId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // コンストラクタ
    public TodoEntity() {
    }

    public TodoEntity(UUID userId, String title, String description, TodoStatus status, 
                      TodoPriority priority, LocalDate dueDate) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public TodoEntity(UUID userId, String title, String description, TodoStatus status,
                      TodoPriority priority, LocalDate dueDate, Long parentId) {
        this(userId, title, description, status, priority, dueDate);
        this.parentId = parentId;
    }

    public TodoEntity(UUID userId, String title, String description, TodoStatus status,
                      TodoPriority priority, LocalDate dueDate, Long parentId,
                      Boolean isRepeatable, RepeatType repeatType, Integer repeatInterval,
                      String repeatDaysOfWeek, Integer repeatDayOfMonth, LocalDate repeatEndDate,
                      Long originalTodoId) {
        this(userId, title, description, status, priority, dueDate, parentId);
        this.isRepeatable = isRepeatable;
        this.repeatType = repeatType;
        this.repeatInterval = repeatInterval;
        this.repeatDaysOfWeek = repeatDaysOfWeek;
        this.repeatDayOfMonth = repeatDayOfMonth;
        this.repeatEndDate = repeatEndDate;
        this.originalTodoId = originalTodoId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public void setStatus(TodoStatus status) {
        this.status = status;
    }

    public TodoPriority getPriority() {
        return priority;
    }

    public void setPriority(TodoPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public TodoEntity getParent() {
        return parent;
    }

    public void setParent(TodoEntity parent) {
        this.parent = parent;
    }

    public java.util.List<TodoEntity> getChildren() {
        return children;
    }

    public void setChildren(java.util.List<TodoEntity> children) {
        this.children = children;
    }

    public Boolean getIsRepeatable() {
        return isRepeatable;
    }

    public void setIsRepeatable(Boolean isRepeatable) {
        this.isRepeatable = isRepeatable;
    }

    public RepeatType getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(RepeatType repeatType) {
        this.repeatType = repeatType;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public String getRepeatDaysOfWeek() {
        return repeatDaysOfWeek;
    }

    public void setRepeatDaysOfWeek(String repeatDaysOfWeek) {
        this.repeatDaysOfWeek = repeatDaysOfWeek;
    }

    public Integer getRepeatDayOfMonth() {
        return repeatDayOfMonth;
    }

    public void setRepeatDayOfMonth(Integer repeatDayOfMonth) {
        this.repeatDayOfMonth = repeatDayOfMonth;
    }

    public LocalDate getRepeatEndDate() {
        return repeatEndDate;
    }

    public void setRepeatEndDate(LocalDate repeatEndDate) {
        this.repeatEndDate = repeatEndDate;
    }

    public Long getOriginalTodoId() {
        return originalTodoId;
    }

    public void setOriginalTodoId(Long originalTodoId) {
        this.originalTodoId = originalTodoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TodoEntity that = (TodoEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TodoEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", dueDate=" + dueDate +
                ", parentId=" + parentId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}