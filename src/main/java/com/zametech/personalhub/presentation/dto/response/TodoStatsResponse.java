package com.zametech.personalhub.presentation.dto.response;

public record TodoStatsResponse(
        long totalTodos,
        long completedTodos,
        long inProgressTodos,
        long pendingTodos,
        double completionRate,
        long overdueCount
) {
}