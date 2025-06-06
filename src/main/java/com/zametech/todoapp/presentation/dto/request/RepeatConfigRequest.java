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
    
    @Pattern(regexp = "^[1-7](,[1-7])*$", message = "曜日は1-7の範囲でカンマ区切りで指定してください")
    String daysOfWeek,
    
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
     * 曜日文字列をリストに変換
     */
    public List<Integer> getDaysOfWeekList() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return List.of();
        }
        return List.of(daysOfWeek.split(","))
                .stream()
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }
}