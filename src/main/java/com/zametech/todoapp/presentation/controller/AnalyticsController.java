package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.AnalyticsService;
import com.zametech.todoapp.presentation.dto.response.DashboardResponse;
import com.zametech.todoapp.presentation.dto.response.TodoActivityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("Getting dashboard analytics");
        DashboardResponse response = analyticsService.getDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/todos/activity")
    public ResponseEntity<TodoActivityResponse> getTodoActivity() {
        log.info("Getting TODO activity analytics");
        TodoActivityResponse response = analyticsService.getTodoActivity();
        return ResponseEntity.ok(response);
    }
}