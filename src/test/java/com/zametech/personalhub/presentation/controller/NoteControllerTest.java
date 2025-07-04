package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.NoteService;
import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.presentation.dto.request.CreateNoteRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateNoteRequest;
import com.zametech.personalhub.presentation.dto.response.NoteResponse;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NoteController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NoteService noteService;

    private Note sampleNote;
    private CreateNoteRequest createNoteRequest;
    private UpdateNoteRequest updateNoteRequest;

    @BeforeEach
    void setUp() {
        sampleNote = new Note();
        sampleNote.setId(1L);
        sampleNote.setTitle("Test Note");
        sampleNote.setContent("Test Content");
        sampleNote.setTags(Arrays.asList("tag1", "tag2"));
        sampleNote.setUserId(UUID.randomUUID());
        sampleNote.setCreatedAt(LocalDateTime.now());
        sampleNote.setUpdatedAt(LocalDateTime.now());

        createNoteRequest = new CreateNoteRequest(
            "New Note",
            "New Content",
            Arrays.asList("new", "test")
        );

        updateNoteRequest = new UpdateNoteRequest(
            "Updated Note",
            "Updated Content",
            Arrays.asList("updated", "test")
        );
    }

    @Test
    @WithMockUser
    void createNote_withValidRequest_shouldReturnCreatedNote() throws Exception {
        when(noteService.createNote(org.mockito.ArgumentMatchers.any(CreateNoteRequest.class))).thenReturn(sampleNote);

        mockMvc.perform(post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createNoteRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test Note"))
            .andExpect(jsonPath("$.content").value("Test Content"))
            .andExpect(jsonPath("$.tags", hasSize(2)))
            .andExpect(jsonPath("$.tags[0]").value("tag1"))
            .andExpect(jsonPath("$.tags[1]").value("tag2"));

        verify(noteService).createNote(org.mockito.ArgumentMatchers.any(CreateNoteRequest.class));
    }

    @Test
    @WithMockUser
    void createNote_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateNoteRequest invalidRequest = new CreateNoteRequest(
            "", // Empty title
            null,
            null
        );

        mockMvc.perform(post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(noteService, never()).createNote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void getNote_withValidId_shouldReturnNote() throws Exception {
        when(noteService.getNoteById(1L)).thenReturn(sampleNote);

        mockMvc.perform(get("/api/v1/notes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Test Note"))
            .andExpect(jsonPath("$.content").value("Test Content"));

        verify(noteService).getNoteById(1L);
    }

    @Test
    @WithMockUser
    void getNote_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(noteService.getNoteById(999L)).thenThrow(new TodoNotFoundException("Note not found with id: 999"));

        mockMvc.perform(get("/api/v1/notes/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Note not found with id: 999"));

        verify(noteService).getNoteById(999L);
    }

    @Test
    @WithMockUser
    void getNote_withAccessDenied_shouldReturnForbidden() throws Exception {
        when(noteService.getNoteById(1L)).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/notes/1"))
            .andExpect(status().isForbidden());

        verify(noteService).getNoteById(1L);
    }

    @Test
    @WithMockUser
    void getNotes_withPagination_shouldReturnPagedNotes() throws Exception {
        List<Note> noteList = Arrays.asList(sampleNote);
        Page<Note> notePage = new PageImpl<>(noteList, PageRequest.of(0, 10), 1);
        
        when(noteService.getNotesByUser(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(notePage);

        mockMvc.perform(get("/api/v1/notes")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0));

        verify(noteService).getNotesByUser(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getNotes_withEmptyResult_shouldReturnEmptyPage() throws Exception {
        Page<Note> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        
        when(noteService.getNotesByUser(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/notes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(noteService).getNotesByUser(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchNotes_withQuery_shouldReturnMatchingNotes() throws Exception {
        List<Note> searchResults = Arrays.asList(sampleNote);
        when(noteService.searchNotes("test")).thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/notes/search")
                .param("query", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Test Note"));

        verify(noteService).searchNotes("test");
    }

    @Test
    @WithMockUser
    void searchNotes_withEmptyQuery_shouldReturnEmptyList() throws Exception {
        when(noteService.searchNotes("")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/notes/search")
                .param("query", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(noteService).searchNotes("");
    }

    @Test
    @WithMockUser
    void getNotesByTag_withValidTag_shouldReturnMatchingNotes() throws Exception {
        List<Note> taggedNotes = Arrays.asList(sampleNote);
        when(noteService.getNotesByTag("tag1")).thenReturn(taggedNotes);

        mockMvc.perform(get("/api/v1/notes/tag/tag1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].tags", hasItem("tag1")));

        verify(noteService).getNotesByTag("tag1");
    }

    @Test
    @WithMockUser
    void getNotesByTag_withNonExistentTag_shouldReturnEmptyList() throws Exception {
        when(noteService.getNotesByTag("nonexistent")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/notes/tag/nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(noteService).getNotesByTag("nonexistent");
    }

    @Test
    @WithMockUser
    void updateNote_withValidRequest_shouldReturnUpdatedNote() throws Exception {
        Note updatedNote = new Note();
        updatedNote.setId(1L);
        updatedNote.setTitle("Updated Note");
        updatedNote.setContent("Updated Content");
        updatedNote.setTags(Arrays.asList("updated", "test"));
        updatedNote.setUserId(sampleNote.getUserId());
        updatedNote.setCreatedAt(sampleNote.getCreatedAt());
        updatedNote.setUpdatedAt(LocalDateTime.now());
        
        when(noteService.updateNote(eq(1L), org.mockito.ArgumentMatchers.any(UpdateNoteRequest.class))).thenReturn(updatedNote);

        mockMvc.perform(put("/api/v1/notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateNoteRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Updated Note"))
            .andExpect(jsonPath("$.content").value("Updated Content"));

        verify(noteService).updateNote(eq(1L), org.mockito.ArgumentMatchers.any(UpdateNoteRequest.class));
    }

    @Test
    @WithMockUser
    void updateNote_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(noteService.updateNote(eq(999L), org.mockito.ArgumentMatchers.any(UpdateNoteRequest.class)))
            .thenThrow(new TodoNotFoundException("Note not found with id: 999"));

        mockMvc.perform(put("/api/v1/notes/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateNoteRequest)))
            .andExpect(status().isNotFound());

        verify(noteService).updateNote(eq(999L), org.mockito.ArgumentMatchers.any(UpdateNoteRequest.class));
    }

    // UpdateNoteRequest has no validation constraints, so no bad request test needed

    @Test
    @WithMockUser
    void deleteNote_withValidId_shouldReturnNoContent() throws Exception {
        doNothing().when(noteService).deleteNote(1L);

        mockMvc.perform(delete("/api/v1/notes/1"))
            .andExpect(status().isNoContent());

        verify(noteService).deleteNote(1L);
    }

    @Test
    @WithMockUser
    void deleteNote_withNonExistentId_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException("Note not found with id: 999")).when(noteService).deleteNote(999L);

        mockMvc.perform(delete("/api/v1/notes/999"))
            .andExpect(status().isNotFound());

        verify(noteService).deleteNote(999L);
    }

    @Test
    @WithMockUser
    void deleteNote_withAccessDenied_shouldReturnForbidden() throws Exception {
        doThrow(new AccessDeniedException("Access denied")).when(noteService).deleteNote(1L);

        mockMvc.perform(delete("/api/v1/notes/1"))
            .andExpect(status().isForbidden());

        verify(noteService).deleteNote(1L);
    }
}