package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.domain.repository.NoteRepository;
import com.zametech.personalhub.presentation.dto.request.CreateNoteRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateNoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private NoteService noteService;

    private UUID userId;
    private Note note;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        note = new Note();
        note.setId(1L);
        note.setTitle("Test Note");
        note.setContent("Test Content");
        note.setTags("test,note");
        note.setUserId(userId);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createNote_withValidRequest_shouldCreateAndReturnNote() {
        // Given
        CreateNoteRequest request = new CreateNoteRequest(
            "New Note",
            "This is a new note content",
            Arrays.asList("important", "work")
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note savedNote = invocation.getArgument(0);
            savedNote.setId(2L);
            return savedNote;
        });

        // When
        Note result = noteService.createNote(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("New Note");
        assertThat(result.getContent()).isEqualTo("This is a new note content");
        assertThat(result.getTags()).isEqualTo("important,work");
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void createNote_withEmptyTags_shouldCreateNoteWithEmptyTags() {
        // Given
        CreateNoteRequest request = new CreateNoteRequest(
            "Note without tags",
            "Content without tags",
            Arrays.asList()
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note savedNote = invocation.getArgument(0);
            savedNote.setId(3L);
            return savedNote;
        });

        // When
        Note result = noteService.createNote(request);

        // Then
        assertThat(result.getTags()).isEqualTo("");
    }

    @Test
    void getNoteById_withValidId_shouldReturnNote() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(note));

        // When
        Note result = noteService.getNoteById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Note");
    }

    @Test
    void getNoteById_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.getNoteById(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Note not found with id: 999");
    }

    @Test
    void getNoteById_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(noteRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.getNoteById(1L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Note not found with id: 1");
    }

    @Test
    void getNotesByUser_shouldReturnPagedNotes() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Note> noteList = Arrays.asList(note);
        Page<Note> notePage = new PageImpl<>(noteList, pageRequest, 1);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByUserId(userId, pageRequest)).thenReturn(notePage);

        // When
        Page<Note> result = noteService.getNotesByUser(pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Note");
    }

    @Test
    void searchNotes_withQuery_shouldReturnMatchingNotes() {
        // Given
        String query = "test";
        Note note2 = new Note();
        note2.setId(2L);
        note2.setTitle("Another test note");
        note2.setContent("Content with test keyword");
        note2.setUserId(userId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.searchByTitleOrContent(userId, query))
            .thenReturn(Arrays.asList(note, note2));

        // When
        List<Note> result = noteService.searchNotes(query);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Note::getTitle)
            .containsExactly("Test Note", "Another test note");
    }

    @Test
    void searchNotes_withNoMatches_shouldReturnEmptyList() {
        // Given
        String query = "nonexistent";
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.searchByTitleOrContent(userId, query))
            .thenReturn(Arrays.asList());

        // When
        List<Note> result = noteService.searchNotes(query);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getNotesByTag_shouldReturnNotesWithTag() {
        // Given
        String tag = "important";
        Note note2 = new Note();
        note2.setId(2L);
        note2.setTitle("Important Note");
        note2.setTags("important,urgent");
        note2.setUserId(userId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByTag(userId, tag))
            .thenReturn(Arrays.asList(note2));

        // When
        List<Note> result = noteService.getNotesByTag(tag);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTags()).contains("important");
    }

    @Test
    void updateNote_withFullUpdate_shouldUpdateAllFields() {
        // Given
        UpdateNoteRequest request = new UpdateNoteRequest(
            "Updated Title",
            "Updated Content",
            Arrays.asList("updated", "modified")
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Note result = noteService.updateNote(1L, request);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getContent()).isEqualTo("Updated Content");
        assertThat(result.getTags()).isEqualTo("updated,modified");
        verify(noteRepository).save(note);
    }

    @Test
    void updateNote_withPartialUpdate_shouldUpdateOnlyProvidedFields() {
        // Given
        UpdateNoteRequest request = new UpdateNoteRequest(
            "Only Title Updated",
            null,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Note result = noteService.updateNote(1L, request);

        // Then
        assertThat(result.getTitle()).isEqualTo("Only Title Updated");
        assertThat(result.getContent()).isEqualTo("Test Content"); // Unchanged
        assertThat(result.getTags()).isEqualTo("test,note"); // Unchanged
        verify(noteRepository).save(note);
    }

    @Test
    void updateNote_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        UpdateNoteRequest request = new UpdateNoteRequest(
            "Updated Title",
            null,
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.updateNote(999L, request))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Note not found with id: 999");
    }

    @Test
    void deleteNote_withValidId_shouldDeleteNote() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(note));

        // When
        noteService.deleteNote(1L);

        // Then
        verify(noteRepository).deleteById(1L);
    }

    @Test
    void deleteNote_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(noteRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.deleteNote(999L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Note not found with id: 999");
    }

    @Test
    void deleteNote_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(noteRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.deleteNote(1L))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("Note not found with id: 1");
    }
}