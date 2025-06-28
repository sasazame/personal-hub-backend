package com.zametech.todoapp.application.goal.dto;

import java.util.List;

public record GroupedGoalsResponse(
        List<GoalResponse> daily,
        List<GoalResponse> weekly,
        List<GoalResponse> monthly,
        List<GoalResponse> annual
) {}