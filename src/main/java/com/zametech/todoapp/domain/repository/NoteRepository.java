package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NoteRepository {
    Note save(Note note);
    Optional<Note> findById(Long id);
    Optional<Note> findByIdAndUserId(Long id, Long userId);
    Page<Note> findByUserId(Long userId, Pageable pageable);
    List<Note> searchByTitleOrContent(Long userId, String query);
    List<Note> findByTag(Long userId, String tag);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
}