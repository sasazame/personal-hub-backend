package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.TodoService;
import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.TodoPriority;
import com.zametech.personalhub.domain.model.TodoStatus;
import com.zametech.personalhub.presentation.dto.request.CreateTodoRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateTodoRequest;
import com.zametech.personalhub.presentation.dto.response.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TodoController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    private TodoResponse sampleTodoResponse;
    private CreateTodoRequest createTodoRequest;
    private UpdateTodoRequest updateTodoRequest;

    @BeforeEach
    void setUp() {
        sampleTodoResponse = new TodoResponse(
            1L,
            "Test TODO",
            "Test Description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            LocalDate.now().plusDays(7),
            null, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );

        createTodoRequest = new CreateTodoRequest(
            "New TODO",
            "New Description",
            TodoPriority.HIGH,
            LocalDate.now().plusDays(3),
            null,
            false,
            null
        );

        updateTodoRequest = new UpdateTodoRequest(
            "Updated TODO",
            "Updated Description",
            TodoStatus.IN_PROGRESS,
            TodoPriority.LOW,
            LocalDate.now().plusDays(5),
            null,
            false,
            null
        );
    }

    @Test
    @WithMockUser
    void createTodo_withValidRequest_shouldReturnCreatedTodo() throws Exception {
        when(todoService.createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class))).thenReturn(sampleTodoResponse);

        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createTodoRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test TODO"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"));

        verify(todoService).createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class));
    }

    @Test
    @WithMockUser
    void createTodo_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateTodoRequest invalidRequest = new CreateTodoRequest(
            "", // Empty title
            null,
            null,
            null,
            null,
            false,
            null
        );

        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(todoService, never()).createTodo(org.mockito.ArgumentMatchers.any());
    }

    // Note: TestSecurityConfig allows all requests, so we can't test authentication failure in unit tests
    // This would be better tested in integration tests

    @Test
    @WithMockUser
    void getTodo_withValidId_shouldReturnTodo() throws Exception {
        when(todoService.getTodo(1L)).thenReturn(sampleTodoResponse);

        mockMvc.perform(get("/api/v1/todos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test TODO"))
            .andExpect(jsonPath("$.description").value("Test Description"));

        verify(todoService).getTodo(1L);
    }

    @Test
    @WithMockUser
    void getTodo_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(todoService.getTodo(999L)).thenThrow(new TodoNotFoundException(999L));

        mockMvc.perform(get("/api/v1/todos/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("TODO not found with id: 999"));

        verify(todoService).getTodo(999L);
    }

    @Test
    @WithMockUser
    void getTodo_withAccessDenied_shouldReturnForbidden() throws Exception {
        when(todoService.getTodo(1L)).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/todos/1"))
            .andExpect(status().isForbidden());

        verify(todoService).getTodo(1L);
    }

    @Test
    @WithMockUser
    void getTodos_withPagination_shouldReturnPagedTodos() throws Exception {
        List<TodoResponse> todoList = Arrays.asList(sampleTodoResponse);
        Page<TodoResponse> todoPage = new PageImpl<>(todoList, PageRequest.of(0, 10), 1);
        
        when(todoService.getTodos(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(todoPage);

        mockMvc.perform(get("/api/v1/todos")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0));

        verify(todoService).getTodos(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getTodos_withEmptyResult_shouldReturnEmptyPage() throws Exception {
        Page<TodoResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        
        when(todoService.getTodos(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(todoService).getTodos(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getTodosByStatus_withValidStatus_shouldReturnFilteredTodos() throws Exception {
        TodoResponse doneTodo = new TodoResponse(
            2L,
            "Done TODO",
            null,
            TodoStatus.DONE,
            TodoPriority.LOW,
            null,
            null, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        
        when(todoService.getTodosByStatus(TodoStatus.DONE)).thenReturn(Arrays.asList(doneTodo));

        mockMvc.perform(get("/api/v1/todos/status/DONE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].status").value("DONE"));

        verify(todoService).getTodosByStatus(TodoStatus.DONE);
    }

    @Test
    @WithMockUser
    void getTodosByStatus_withInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/todos/status/INVALID"))
            .andExpect(status().isBadRequest());

        verify(todoService, never()).getTodosByStatus(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void updateTodo_withValidRequest_shouldReturnUpdatedTodo() throws Exception {
        TodoResponse updatedResponse = new TodoResponse(
            1L,
            "Updated TODO",
            "Updated Description",
            TodoStatus.IN_PROGRESS,
            TodoPriority.LOW,
            LocalDate.now().plusDays(5),
            null, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        
        when(todoService.updateTodo(eq(1L), org.mockito.ArgumentMatchers.any(UpdateTodoRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/todos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateTodoRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Updated TODO"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.priority").value("LOW"));

        verify(todoService).updateTodo(eq(1L), org.mockito.ArgumentMatchers.any(UpdateTodoRequest.class));
    }

    @Test
    @WithMockUser
    void updateTodo_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(todoService.updateTodo(eq(999L), org.mockito.ArgumentMatchers.any(UpdateTodoRequest.class)))
            .thenThrow(new TodoNotFoundException(999L));

        mockMvc.perform(put("/api/v1/todos/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateTodoRequest)))
            .andExpect(status().isNotFound());

        verify(todoService).updateTodo(eq(999L), org.mockito.ArgumentMatchers.any(UpdateTodoRequest.class));
    }

    @Test
    @WithMockUser
    void updateTodo_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        UpdateTodoRequest invalidRequest = new UpdateTodoRequest(
            "", // Empty title
            null,
            null,
            null,
            null,
            null,
            false,
            null
        );

        mockMvc.perform(put("/api/v1/todos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(todoService, never()).updateTodo(anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void deleteTodo_withValidId_shouldReturnNoContent() throws Exception {
        doNothing().when(todoService).deleteTodo(1L);

        mockMvc.perform(delete("/api/v1/todos/1"))
            .andExpect(status().isNoContent());

        verify(todoService).deleteTodo(1L);
    }

    @Test
    @WithMockUser
    void deleteTodo_withNonExistentId_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException(999L)).when(todoService).deleteTodo(999L);

        mockMvc.perform(delete("/api/v1/todos/999"))
            .andExpect(status().isNotFound());

        verify(todoService).deleteTodo(999L);
    }

    @Test
    @WithMockUser
    void deleteTodo_withAccessDenied_shouldReturnForbidden() throws Exception {
        doThrow(new AccessDeniedException("Access denied")).when(todoService).deleteTodo(1L);

        mockMvc.perform(delete("/api/v1/todos/1"))
            .andExpect(status().isForbidden());

        verify(todoService).deleteTodo(1L);
    }

    @Test
    @WithMockUser
    void getChildTasks_withValidParentId_shouldReturnChildTodos() throws Exception {
        TodoResponse childTodo1 = new TodoResponse(
            2L,
            "Child TODO 1",
            null,
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            1L, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        TodoResponse childTodo2 = new TodoResponse(
            3L,
            "Child TODO 2",
            null,
            TodoStatus.TODO,
            TodoPriority.LOW,
            null,
            1L, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        
        when(todoService.getChildTasks(1L)).thenReturn(Arrays.asList(childTodo1, childTodo2));

        mockMvc.perform(get("/api/v1/todos/1/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].parentId").value(1))
            .andExpect(jsonPath("$[1].id").value(3))
            .andExpect(jsonPath("$[1].parentId").value(1));

        verify(todoService).getChildTasks(1L);
    }

    @Test
    @WithMockUser
    void getChildTasks_withNoChildren_shouldReturnEmptyList() throws Exception {
        when(todoService.getChildTasks(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/todos/1/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(todoService).getChildTasks(1L);
    }

    @Test
    @WithMockUser
    void getChildTasks_withNonExistentParent_shouldReturnNotFound() throws Exception {
        when(todoService.getChildTasks(999L)).thenThrow(new TodoNotFoundException(999L));

        mockMvc.perform(get("/api/v1/todos/999/children"))
            .andExpect(status().isNotFound());

        verify(todoService).getChildTasks(999L);
    }

    @Test
    @WithMockUser
    void createTodo_withParentId_shouldReturnCreatedTodoWithParent() throws Exception {
        CreateTodoRequest requestWithParent = new CreateTodoRequest(
            "Child TODO",
            "Child Description",
            TodoPriority.MEDIUM,
            null,
            1L, // Parent ID
            false,
            null
        );
        
        TodoResponse responseWithParent = new TodoResponse(
            2L,
            "Child TODO",
            "Child Description",
            TodoStatus.TODO,
            TodoPriority.MEDIUM,
            null,
            1L, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        
        when(todoService.createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class))).thenReturn(responseWithParent);

        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithParent)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.parentId").value(1));

        verify(todoService).createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class));
    }

    @Test
    @WithMockUser
    void createTodo_withoutDueDate_shouldReturnCreatedTodo() throws Exception {
        CreateTodoRequest requestWithoutDueDate = new CreateTodoRequest(
            "TODO without due date",
            null,
            TodoPriority.LOW,
            null, // No due date
            null,
            false,
            null
        );
        
        TodoResponse responseWithoutDueDate = new TodoResponse(
            3L,
            "TODO without due date",
            null,
            TodoStatus.TODO,
            TodoPriority.LOW,
            null,
            null, // parentId
            false, // isRepeatable
            null, // repeatConfig
            null, // originalTodoId
            ZonedDateTime.now(),
            ZonedDateTime.now()
        );
        
        when(todoService.createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class))).thenReturn(responseWithoutDueDate);

        mockMvc.perform(post("/api/v1/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithoutDueDate)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.dueDate").doesNotExist());

        verify(todoService).createTodo(org.mockito.ArgumentMatchers.any(CreateTodoRequest.class));
    }
}