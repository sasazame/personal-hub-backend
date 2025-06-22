package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository {
    Note save(Note note);
    Optional<Note> findById(Long id);
    Optional<Note> findByIdAndUserId(Long id, UUID userId);
    Page<Note> findByUserId(UUID userId, Pageable pageable);
    List<Note> searchByTitleOrContent(UUID userId, String query);
    List<Note> findByTag(UUID userId, String tag);
    void deleteById(Long id);
    void deleteByUserId(UUID userId);
}