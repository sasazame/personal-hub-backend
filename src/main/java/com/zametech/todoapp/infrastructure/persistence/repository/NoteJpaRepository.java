package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.infrastructure.persistence.entity.NoteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteJpaRepository extends JpaRepository<NoteEntity, Long> {
    
    Optional<NoteEntity> findByIdAndUserId(Long id, Long userId);
    
    Page<NoteEntity> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT n FROM NoteEntity n WHERE n.userId = :userId AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<NoteEntity> searchByTitleOrContent(@Param("userId") Long userId, @Param("query") String query);
    
    @Query("SELECT n FROM NoteEntity n WHERE n.userId = :userId AND " +
           "(n.tags LIKE CONCAT('%', :tag, '%'))")
    List<NoteEntity> findByTag(@Param("userId") Long userId, @Param("tag") String tag);
    
    void deleteByUserId(Long userId);
}