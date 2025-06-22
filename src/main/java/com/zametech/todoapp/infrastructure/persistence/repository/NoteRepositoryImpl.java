package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.Note;
import com.zametech.todoapp.domain.repository.NoteRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.NoteEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NoteRepositoryImpl implements NoteRepository {

    private final NoteJpaRepository noteJpaRepository;

    @Override
    public Note save(Note note) {
        NoteEntity entity = toEntity(note);
        NoteEntity savedEntity = noteJpaRepository.save(entity);
        return toModel(savedEntity);
    }

    @Override
    public Optional<Note> findById(Long id) {
        return noteJpaRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    public Optional<Note> findByIdAndUserId(Long id, UUID userId) {
        return noteJpaRepository.findByIdAndUserId(id, userId)
                .map(this::toModel);
    }

    @Override
    public Page<Note> findByUserId(UUID userId, Pageable pageable) {
        return noteJpaRepository.findByUserId(userId, pageable)
                .map(this::toModel);
    }

    @Override
    public List<Note> searchByTitleOrContent(UUID userId, String query) {
        return noteJpaRepository.searchByTitleOrContent(userId, query)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<Note> findByTag(UUID userId, String tag) {
        return noteJpaRepository.findByTag(userId, tag)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        noteJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        noteJpaRepository.deleteByUserId(userId);
    }

    private NoteEntity toEntity(Note note) {
        NoteEntity entity = new NoteEntity();
        entity.setId(note.getId());
        entity.setTitle(note.getTitle());
        entity.setContent(note.getContent());
        entity.setTags(note.getTags());
        entity.setUserId(note.getUserId());
        entity.setCreatedAt(note.getCreatedAt());
        entity.setUpdatedAt(note.getUpdatedAt());
        return entity;
    }

    private Note toModel(NoteEntity entity) {
        return new Note(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getTags(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}