package com.zametech.todoapp.presentation.dto.request;

import com.zametech.todoapp.domain.model.TodoPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * TODO作成リクエスト
 */
public record CreateTodoRequest(
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    String title,
    
    @Size(max = 1000, message = "説明は1000文字以内で入力してください")
    String description,
    
    TodoPriority priority,
    
    LocalDate dueDate,
    
    Long parentId
) {
    public CreateTodoRequest {
        // デフォルト値の設定
        if (priority == null) {
            priority = TodoPriority.MEDIUM;
        }
    }
}