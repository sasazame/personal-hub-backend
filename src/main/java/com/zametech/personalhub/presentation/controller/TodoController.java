package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.TodoService;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.presentation.dto.request.CreateTodoRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateTodoRequest;
import com.zametech.personalhub.presentation.dto.response.TodoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TODO REST APIコントローラー
 */
@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;

    /**
     * TODO作成
     */
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        log.info("POST /api/v1/todos - Creating new TODO");
        TodoResponse response = todoService.createTodo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * TODO取得（ID指定）
     */
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable Long id) {
        log.info("GET /api/v1/todos/{} - Getting TODO", id);
        TodoResponse response = todoService.getTodo(id);
        return ResponseEntity.ok(response);
    }

    /**
     * TODO一覧取得（ページング）
     */
    @GetMapping
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /api/v1/todos - Getting TODO list");
        Page<TodoResponse> response = todoService.getTodos(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * ステータスでTODO一覧取得
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TodoResponse>> getTodosByStatus(@PathVariable TodoStatus status) {
        log.info("GET /api/v1/todos/status/{} - Getting TODOs by status", status);
        List<TodoResponse> response = todoService.getTodosByStatus(status);
        return ResponseEntity.ok(response);
    }

    /**
     * TODO更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTodoRequest request) {
        log.info("PUT /api/v1/todos/{} - Updating TODO", id);
        TodoResponse response = todoService.updateTodo(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * TODO削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        log.info("DELETE /api/v1/todos/{} - Deleting TODO", id);
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 子タスク一覧取得
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<TodoResponse>> getChildTasks(@PathVariable Long parentId) {
        log.info("GET /api/v1/todos/{}/children - Getting child tasks", parentId);
        List<TodoResponse> response = todoService.getChildTasks(parentId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 繰り返し可能なTODO一覧取得
     */
    @GetMapping("/repeatable")
    public ResponseEntity<List<TodoResponse>> getRepeatableTodos() {
        log.info("GET /api/v1/todos/repeatable - Getting repeatable TODOs");
        List<TodoResponse> response = todoService.getRepeatableTodos();
        return ResponseEntity.ok(response);
    }
    
    /**
     * 特定の繰り返しTODOから生成されたインスタンス一覧を取得
     */
    @GetMapping("/{originalTodoId}/instances")
    public ResponseEntity<List<TodoResponse>> getRepeatInstances(@PathVariable Long originalTodoId) {
        log.info("GET /api/v1/todos/{}/instances - Getting repeat instances", originalTodoId);
        List<TodoResponse> response = todoService.getRepeatInstances(originalTodoId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 期限到来した繰り返しTODOの新しいインスタンスを生成
     */
    @PostMapping("/repeat/generate")
    public ResponseEntity<List<TodoResponse>> generatePendingRepeatInstances() {
        log.info("POST /api/v1/todos/repeat/generate - Generating pending repeat instances");
        List<TodoResponse> response = todoService.generatePendingRepeatInstances();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * TODOステータス切り替え
     */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<TodoResponse> toggleTodoStatus(@PathVariable Long id) {
        log.info("POST /api/v1/todos/{}/toggle-status - Toggling TODO status", id);
        TodoResponse response = todoService.toggleTodoStatus(id);
        return ResponseEntity.ok(response);
    }
}