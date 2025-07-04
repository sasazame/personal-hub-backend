package com.zametech.personalhub.presentation.dto.request;

import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * TODO更新リクエスト
 */
public record UpdateTodoRequest(
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    String title,
    
    @Size(max = 1000, message = "説明は1000文字以内で入力してください")
    String description,
    
    @NotNull(message = "ステータスは必須です")
    TodoStatus status,
    
    @NotNull(message = "優先度は必須です")
    TodoPriority priority,
    
    LocalDate dueDate,
    
    Long parentId,
    
    Boolean isRepeatable,
    
    @Valid
    RepeatConfigRequest repeatConfig
) {
    public UpdateTodoRequest {
        // 繰り返し設定の妥当性チェック
        if (Boolean.TRUE.equals(isRepeatable) && repeatConfig == null) {
            throw new IllegalArgumentException("繰り返し設定が有効な場合、繰り返し設定は必須です");
        }
        if (Boolean.FALSE.equals(isRepeatable) && repeatConfig != null) {
            throw new IllegalArgumentException("繰り返し設定が無効な場合、繰り返し設定は指定できません");
        }
    }
}