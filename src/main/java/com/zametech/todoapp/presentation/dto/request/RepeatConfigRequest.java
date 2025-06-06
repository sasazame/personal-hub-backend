package com.zametech.todoapp.presentation.dto.request;

import com.zametech.todoapp.domain.model.RepeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * 繰り返し設定リクエスト
 */
public record RepeatConfigRequest(
    @NotNull(message = "繰り返しタイプは必須です")
    RepeatType repeatType,
    
    @Min(value = 1, message = "間隔は1以上で指定してください")
    Integer interval,
    
    List<Integer> daysOfWeek,
    
    @Min(value = 1, message = "日付は1以上で指定してください")
    Integer dayOfMonth,
    
    LocalDate endDate
) {
    public RepeatConfigRequest {
        // デフォルト値の設定
        if (interval == null && (repeatType == RepeatType.DAILY || repeatType == RepeatType.WEEKLY || 
                                 repeatType == RepeatType.MONTHLY || repeatType == RepeatType.YEARLY)) {
            interval = 1;
        }
    }
    
    /**
     * 曜日リストを文字列に変換（データベース保存用）
     */
    public String getDaysOfWeekString() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }
        return daysOfWeek.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}