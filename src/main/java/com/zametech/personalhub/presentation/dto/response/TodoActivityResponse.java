package com.zametech.personalhub.presentation.dto.response;

import java.util.List;
import java.util.Map;

public record TodoActivityResponse(
        List<DailyCount> dailyCompletions,
        List<DailyCount> dailyCreations,
        Map<String, Integer> priorityDistribution,
        Map<String, Integer> statusDistribution,
        double averageCompletionTimeInDays
) {
}