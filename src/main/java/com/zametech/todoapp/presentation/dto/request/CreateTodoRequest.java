package com.zametech.todoapp.presentation.dto.request;

import com.zametech.todoapp.domain.model.TodoPriority;
import jakarta.validation.Valid;
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
    
    Long parentId,
    
    Boolean isRepeatable,
    
    @Valid
    RepeatConfigRequest repeatConfig
) {
    public CreateTodoRequest {
        // デフォルト値の設定
        if (priority == null) {
            priority = TodoPriority.MEDIUM;
        }
        if (isRepeatable == null) {
            isRepeatable = false;
        }
        
        // 繰り返し設定の妥当性チェック
        if (Boolean.TRUE.equals(isRepeatable) && repeatConfig == null) {
            throw new IllegalArgumentException("繰り返し設定が有効な場合、繰り返し設定は必須です");
        }
        if (Boolean.FALSE.equals(isRepeatable) && repeatConfig != null) {
            throw new IllegalArgumentException("繰り返し設定が無効な場合、繰り返し設定は指定できません");
        }
    }
}