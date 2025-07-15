package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.PomodoroService;
import com.zametech.personalhub.presentation.dto.request.*;
import com.zametech.personalhub.presentation.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Pomodoro timer functionality.
 */
@RestController
@RequestMapping("/api/v1/pomodoro")
public class PomodoroController {
    private final PomodoroService pomodoroService;

    public PomodoroController(PomodoroService pomodoroService) {
        this.pomodoroService = pomodoroService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<PomodoroSessionResponse> createSession(@Valid @RequestBody CreatePomodoroSessionRequest request) {
        PomodoroSessionResponse response = pomodoroService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<PomodoroSessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdatePomodoroSessionRequest request) {
        PomodoroSessionResponse response = pomodoroService.updateSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<PomodoroSessionResponse> getActiveSession() {
        PomodoroSessionResponse response = pomodoroService.getActiveSession();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<PomodoroSessionResponse>> getSessionHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<PomodoroSessionResponse> response = pomodoroService.getSessionHistory(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/tasks")
    public ResponseEntity<List<PomodoroTaskResponse>> getSessionTasks(@PathVariable UUID sessionId) {
        List<PomodoroTaskResponse> response = pomodoroService.getSessionTasks(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/tasks")
    public ResponseEntity<PomodoroTaskResponse> addTaskToSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreatePomodoroTaskRequest request) {
        PomodoroTaskResponse response = pomodoroService.addTaskToSession(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/sessions/{sessionId}/tasks/{taskId}")
    public ResponseEntity<PomodoroTaskResponse> updateTask(
            @PathVariable UUID sessionId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdatePomodoroTaskRequest request) {
        PomodoroTaskResponse response = pomodoroService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{sessionId}/tasks/{taskId}")
    public ResponseEntity<Void> removeTask(
            @PathVariable UUID sessionId,
            @PathVariable UUID taskId) {
        pomodoroService.removeTaskFromSession(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/config")
    public ResponseEntity<PomodoroConfigResponse> getConfig() {
        PomodoroConfigResponse response = pomodoroService.getConfig();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/config")
    public ResponseEntity<PomodoroConfigResponse> updateConfig(@Valid @RequestBody UpdatePomodoroConfigRequest request) {
        PomodoroConfigResponse response = pomodoroService.updateConfig(request);
        return ResponseEntity.ok(response);
    }
}