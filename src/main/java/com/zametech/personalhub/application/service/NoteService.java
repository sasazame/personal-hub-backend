package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.domain.repository.NoteRepository;
import com.zametech.personalhub.presentation.dto.request.CreateNoteRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateNoteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserContextService userContextService;

    @Transactional
    public Note createNote(CreateNoteRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Note note = new Note();
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTags(request.tags());
        note.setUserId(currentUserId);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        log.info("Creating new note: {} for user: {}", request.title(), currentUserId);
        return noteRepository.save(note);
    }

    public Note getNoteById(Long noteId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Note note = noteRepository.findByIdAndUserId(noteId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Note not found with id: " + noteId));
        
        log.info("Getting note with id: {} for user: {}", noteId, currentUserId);
        return note;
    }

    public Page<Note> getNotesByUser(Pageable pageable) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting notes for user: {} with pageable: {}", currentUserId, pageable);
        return noteRepository.findByUserId(currentUserId, pageable);
    }

    public List<Note> searchNotes(String query) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Searching notes for user: {} with query: {}", currentUserId, query);
        return noteRepository.searchByTitleOrContent(currentUserId, query);
    }

    public List<Note> getNotesByTag(String tag) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting notes by tag: {} for user: {}", tag, currentUserId);
        return noteRepository.findByTag(currentUserId, tag);
    }

    @Transactional
    public Note updateNote(Long noteId, UpdateNoteRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Note note = noteRepository.findByIdAndUserId(noteId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Note not found with id: " + noteId));

        if (request.title() != null) {
            note.setTitle(request.title());
        }
        if (request.content() != null) {
            note.setContent(request.content());
        }
        if (request.tags() != null) {
            note.setTags(request.tags());
        }

        log.info("Updating note with id: {} for user: {}", noteId, currentUserId);
        return noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Note note = noteRepository.findByIdAndUserId(noteId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Note not found with id: " + noteId));

        log.info("Deleting note with id: {} for user: {}", noteId, currentUserId);
        noteRepository.deleteById(noteId);
    }
}