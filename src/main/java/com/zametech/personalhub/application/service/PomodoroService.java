package com.zametech.personalhub.application.service;

import com.zametech.personalhub.domain.exception.ActiveSessionNotFoundException;
import com.zametech.personalhub.domain.model.*;
import com.zametech.personalhub.domain.repository.*;
import com.zametech.personalhub.presentation.dto.request.*;
import com.zametech.personalhub.presentation.dto.response.*;
import com.zametech.personalhub.shared.constants.AlarmSound;
import com.zametech.personalhub.shared.constants.SessionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing Pomodoro timer functionality.
 */
@Service
@Transactional
public class PomodoroService {
    private final PomodoroSessionRepository sessionRepository;
    private final PomodoroTaskRepository taskRepository;
    private final PomodoroConfigRepository configRepository;
    private final TodoRepository todoRepository;
    private final UserContextService userContextService;

    public PomodoroService(PomodoroSessionRepository sessionRepository,
                          PomodoroTaskRepository taskRepository,
                          PomodoroConfigRepository configRepository,
                          TodoRepository todoRepository,
                          UserContextService userContextService) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.configRepository = configRepository;
        this.todoRepository = todoRepository;
        this.userContextService = userContextService;
    }

    /**
     * Creates a new Pomodoro session for the current user.
     */
    public PomodoroSessionResponse createSession(CreatePomodoroSessionRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        // Check if user already has an active session
        sessionRepository.findActiveSessionByUserId(userId).ifPresent(session -> {
            throw new IllegalStateException("User already has an active Pomodoro session");
        });
        
        // Create new session
        PomodoroSession session = new PomodoroSession(userId, request.getWorkDuration(), request.getBreakDuration());
        session.setStartTime(LocalDateTime.now());
        session = sessionRepository.save(session);
        
        // Add tasks if provided
        if (request.getTasks() != null && !request.getTasks().isEmpty()) {
            List<PomodoroTask> tasks = new ArrayList<>();
            for (int i = 0; i < request.getTasks().size(); i++) {
                CreatePomodoroTaskRequest taskRequest = request.getTasks().get(i);
                PomodoroTask task = new PomodoroTask(session.getId(), taskRequest.getDescription(), i);
                task.setTodoId(taskRequest.getTodoId());
                tasks.add(taskRepository.save(task));
            }
            session.setTasks(tasks);
        }
        
        return toSessionResponse(session);
    }

    /**
     * Updates an existing Pomodoro session.
     */
    public PomodoroSessionResponse updateSession(UUID sessionId, UpdatePomodoroSessionRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to current user");
        }
        
        switch (request.getAction()) {
            case START:
                if (session.getStatus() != PomodoroSession.SessionStatus.ACTIVE) {
                    throw new IllegalStateException("Session is not active");
                }
                session.setStartTime(LocalDateTime.now());
                break;
                
            case PAUSE:
                if (session.getStatus() != PomodoroSession.SessionStatus.ACTIVE) {
                    throw new IllegalStateException("Session is not active");
                }
                session.setStatus(PomodoroSession.SessionStatus.PAUSED);
                break;
                
            case RESUME:
                if (session.getStatus() != PomodoroSession.SessionStatus.PAUSED) {
                    throw new IllegalStateException("Session is not paused");
                }
                session.setStatus(PomodoroSession.SessionStatus.ACTIVE);
                break;
                
            case COMPLETE:
                session.setStatus(PomodoroSession.SessionStatus.COMPLETED);
                session.setEndTime(LocalDateTime.now());
                if (session.getSessionType() == SessionType.WORK) {
                    session.setCompletedCycles(session.getCompletedCycles() + 1);
                }
                break;
                
            case CANCEL:
                session.setStatus(PomodoroSession.SessionStatus.CANCELLED);
                session.setEndTime(LocalDateTime.now());
                break;
                
            case SWITCH_TYPE:
                if (request.getSessionType() != null) {
                    session.setSessionType(request.getSessionType());
                }
                break;
        }
        
        session = sessionRepository.save(session);
        return toSessionResponse(session);
    }

    /**
     * Gets the current active session for the user.
     */
    public PomodoroSessionResponse getActiveSession() {
        UUID userId = userContextService.getCurrentUserId();
        
        return sessionRepository.findActiveSessionByUserId(userId)
                .map(this::toSessionResponse)
                .orElseThrow(() -> new ActiveSessionNotFoundException());
    }

    /**
     * Gets session history for the current user.
     */
    @Transactional(readOnly = true)
    public Page<PomodoroSessionResponse> getSessionHistory(Pageable pageable) {
        UUID userId = userContextService.getCurrentUserId();
        
        return sessionRepository.findByUserId(userId, pageable)
                .map(this::toSessionResponseWithoutTasks);
    }

    /**
     * Updates a task within a session.
     */
    public PomodoroTaskResponse updateTask(UUID taskId, UpdatePomodoroTaskRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Verify task belongs to user's session
        PomodoroSession session = sessionRepository.findById(task.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Task does not belong to current user");
        }
        
        task.setCompleted(request.getCompleted());
        
        // If task is linked to a Todo and is being marked as completed, update the Todo
        if (request.getCompleted() && task.getTodoId() != null) {
            // Note: Todo IDs are Long, not UUID - would need to convert or update Todo to use UUID
            // For now, we'll skip auto-updating the Todo status
        }
        
        task = taskRepository.save(task);
        return toTaskResponse(task);
    }

    /**
     * Gets all tasks for a session.
     */
    public List<PomodoroTaskResponse> getSessionTasks(UUID sessionId) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to current user");
        }
        
        List<PomodoroTask> tasks = taskRepository.findBySessionId(sessionId);
        return tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Adds a task to an existing session.
     */
    public PomodoroTaskResponse addTaskToSession(UUID sessionId, CreatePomodoroTaskRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to current user");
        }
        
        // Get current max order index
        List<PomodoroTask> existingTasks = taskRepository.findBySessionId(sessionId);
        int maxOrderIndex = existingTasks.stream()
                .mapToInt(PomodoroTask::getOrderIndex)
                .max()
                .orElse(-1);
        
        PomodoroTask task = new PomodoroTask(sessionId, request.getDescription(), maxOrderIndex + 1);
        task.setTodoId(request.getTodoId());
        task = taskRepository.save(task);
        
        return toTaskResponse(task);
    }

    /**
     * Removes a task from a session.
     */
    public void removeTaskFromSession(UUID taskId) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        // Verify task belongs to user's session
        PomodoroSession session = sessionRepository.findById(task.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Task does not belong to current user");
        }
        
        taskRepository.deleteById(taskId);
    }

    /**
     * Gets or creates user's Pomodoro configuration.
     */
    public PomodoroConfigResponse getConfig() {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroConfig config = configRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PomodoroConfig newConfig = new PomodoroConfig(userId);
                    return configRepository.save(newConfig);
                });
        
        return toConfigResponse(config);
    }

    /**
     * Updates user's Pomodoro configuration.
     */
    public PomodoroConfigResponse updateConfig(UpdatePomodoroConfigRequest request) {
        UUID userId = userContextService.getCurrentUserId();
        
        PomodoroConfig config = configRepository.findByUserId(userId)
                .orElseGet(() -> new PomodoroConfig(userId));
        
        if (request.getWorkDuration() != null) {
            config.setWorkDuration(request.getWorkDuration());
        }
        if (request.getShortBreakDuration() != null) {
            config.setShortBreakDuration(request.getShortBreakDuration());
        }
        if (request.getLongBreakDuration() != null) {
            config.setLongBreakDuration(request.getLongBreakDuration());
        }
        if (request.getCyclesBeforeLongBreak() != null) {
            config.setCyclesBeforeLongBreak(request.getCyclesBeforeLongBreak());
        }
        if (request.getAlarmSound() != null) {
            config.setAlarmSound(AlarmSound.fromValue(request.getAlarmSound()));
        }
        if (request.getAlarmVolume() != null) {
            config.setAlarmVolume(request.getAlarmVolume());
        }
        if (request.getAutoStartBreaks() != null) {
            config.setAutoStartBreaks(request.getAutoStartBreaks());
        }
        if (request.getAutoStartWork() != null) {
            config.setAutoStartWork(request.getAutoStartWork());
        }
        
        config = configRepository.save(config);
        return toConfigResponse(config);
    }

    // Conversion methods
    private PomodoroSessionResponse toSessionResponse(PomodoroSession session) {
        PomodoroSessionResponse response = new PomodoroSessionResponse();
        response.setId(session.getId());
        response.setUserId(session.getUserId());
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setWorkDuration(session.getWorkDuration());
        response.setBreakDuration(session.getBreakDuration());
        response.setCompletedCycles(session.getCompletedCycles());
        response.setStatus(session.getStatus().name());
        response.setSessionType(session.getSessionType().name());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        
        if (session.getTasks() != null) {
            List<PomodoroTaskResponse> taskResponses = session.getTasks().stream()
                    .map(this::toTaskResponse)
                    .collect(Collectors.toList());
            response.setTasks(taskResponses);
        }
        
        return response;
    }

    private PomodoroSessionResponse toSessionResponseWithoutTasks(PomodoroSession session) {
        PomodoroSessionResponse response = new PomodoroSessionResponse();
        response.setId(session.getId());
        response.setUserId(session.getUserId());
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setWorkDuration(session.getWorkDuration());
        response.setBreakDuration(session.getBreakDuration());
        response.setCompletedCycles(session.getCompletedCycles());
        response.setStatus(session.getStatus().name());
        response.setSessionType(session.getSessionType().name());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        // Don't include tasks to avoid lazy loading issues
        return response;
    }

    private PomodoroTaskResponse toTaskResponse(PomodoroTask task) {
        PomodoroTaskResponse response = new PomodoroTaskResponse();
        response.setId(task.getId());
        response.setSessionId(task.getSessionId());
        response.setTodoId(task.getTodoId());
        response.setDescription(task.getDescription());
        response.setCompleted(task.getCompleted());
        response.setOrderIndex(task.getOrderIndex());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        
        // Optionally load linked Todo
        // Note: Todo IDs are Long, not UUID - would need to convert or update Todo to use UUID
        // For now, we'll skip loading the linked Todo
        
        return response;
    }

    private PomodoroConfigResponse toConfigResponse(PomodoroConfig config) {
        PomodoroConfigResponse response = new PomodoroConfigResponse();
        response.setId(config.getId());
        response.setUserId(config.getUserId());
        response.setWorkDuration(config.getWorkDuration());
        response.setShortBreakDuration(config.getShortBreakDuration());
        response.setLongBreakDuration(config.getLongBreakDuration());
        response.setCyclesBeforeLongBreak(config.getCyclesBeforeLongBreak());
        response.setAlarmSound(config.getAlarmSound().getValue());
        response.setAlarmVolume(config.getAlarmVolume());
        response.setAutoStartBreaks(config.getAutoStartBreaks());
        response.setAutoStartWork(config.getAutoStartWork());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }
}