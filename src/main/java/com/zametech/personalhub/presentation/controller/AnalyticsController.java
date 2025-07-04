package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.AnalyticsService;
import com.zametech.personalhub.presentation.dto.response.DashboardResponse;
import com.zametech.personalhub.presentation.dto.response.TodoActivityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('USER')")
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
    public ResponseEntity<TodoActivityResponse> getTodoActivity(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        
        log.info("Getting TODO activity analytics with days={}, dateFrom={}, dateTo={}", days, dateFrom, dateTo);
        
        // Set default date range if not provided
        LocalDate endDate = dateTo != null ? dateTo : LocalDate.now();
        LocalDate startDate = dateFrom != null ? dateFrom : endDate.minusDays(days);
        
        TodoActivityResponse response = analyticsService.getTodoActivity(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}