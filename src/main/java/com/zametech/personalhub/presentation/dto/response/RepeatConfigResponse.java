package com.zametech.personalhub.presentation.dto.response;

import com.zametech.personalhub.domain.model.RepeatType;
import com.zametech.personalhub.infrastructure.persistence.entity.TodoEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 繰り返し設定レスポンス
 */
public record RepeatConfigResponse(
    RepeatType repeatType,
    Integer interval,
    List<Integer> daysOfWeek,
    Integer dayOfMonth,
    LocalDate endDate
) {
    /**
     * TodoEntityから生成
     */
    public static RepeatConfigResponse from(TodoEntity entity) {
        if (!Boolean.TRUE.equals(entity.getIsRepeatable()) || entity.getRepeatType() == null) {
            return null;
        }
        
        List<Integer> daysOfWeek = null;
        if (entity.getRepeatDaysOfWeek() != null && !entity.getRepeatDaysOfWeek().isEmpty()) {
            daysOfWeek = Arrays.stream(entity.getRepeatDaysOfWeek().split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        }
        
        return new RepeatConfigResponse(
                entity.getRepeatType(),
                entity.getRepeatInterval(),
                daysOfWeek,
                entity.getRepeatDayOfMonth(),
                entity.getRepeatEndDate()
        );
    }
}