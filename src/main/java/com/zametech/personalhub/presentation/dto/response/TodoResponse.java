package com.zametech.personalhub.presentation.dto.response;

import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * TODOレスポンス
 */
public record TodoResponse(
    Long id,
    String title,
    String description,
    TodoStatus status,
    TodoPriority priority,
    LocalDate dueDate,
    Long parentId,
    Boolean isRepeatable,
    RepeatConfigResponse repeatConfig,
    Long originalTodoId,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt
) {
    /**
     * Entityから生成
     */
    public static TodoResponse from(TodoEntity entity) {
        return new TodoResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getDueDate(),
            entity.getParentId(),
            entity.getIsRepeatable(),
            RepeatConfigResponse.from(entity),
            entity.getOriginalTodoId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}