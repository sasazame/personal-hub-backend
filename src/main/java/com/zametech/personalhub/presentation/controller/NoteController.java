package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.NoteService;
import com.zametech.personalhub.domain.model.Note;
import com.zametech.personalhub.presentation.dto.request.CreateNoteRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateNoteRequest;
import com.zametech.personalhub.presentation.dto.response.NoteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody CreateNoteRequest request) {
        log.info("Creating note: {}", request.title());
        Note note = noteService.createNote(request);
        NoteResponse response = mapToNoteResponse(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNote(@PathVariable Long id) {
        log.info("Getting note with id: {}", id);
        Note note = noteService.getNoteById(id);
        NoteResponse response = mapToNoteResponse(note);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<NoteResponse>> getNotes(Pageable pageable) {
        log.info("Getting notes with pageable: {}", pageable);
        Page<Note> notes = noteService.getNotesByUser(pageable);
        Page<NoteResponse> response = notes.map(this::mapToNoteResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<NoteResponse>> searchNotes(@RequestParam String query) {
        log.info("Searching notes with query: {}", query);
        List<Note> notes = noteService.searchNotes(query);
        List<NoteResponse> response = notes.stream()
                .map(this::mapToNoteResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<NoteResponse>> getNotesByTag(@PathVariable String tag) {
        log.info("Getting notes by tag: {}", tag);
        List<Note> notes = noteService.getNotesByTag(tag);
        List<NoteResponse> response = notes.stream()
                .map(this::mapToNoteResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request) {
        log.info("Updating note with id: {}", id);
        Note note = noteService.updateNote(id, request);
        NoteResponse response = mapToNoteResponse(note);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        log.info("Deleting note with id: {}", id);
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }

    private NoteResponse mapToNoteResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getTagList(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}