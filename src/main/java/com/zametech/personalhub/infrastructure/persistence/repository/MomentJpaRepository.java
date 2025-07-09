package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.infrastructure.persistence.entity.MomentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MomentJpaRepository extends JpaRepository<MomentEntity, Long> {
    
    Optional<MomentEntity> findByIdAndUserId(Long id, UUID userId);
    
    Page<MomentEntity> findByUserId(UUID userId, Pageable pageable);
    
    Page<MomentEntity> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT m FROM MomentEntity m WHERE m.userId = :userId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<MomentEntity> searchByContent(@Param("userId") UUID userId, @Param("query") String query);
    
    @Query("SELECT m FROM MomentEntity m WHERE m.userId = :userId AND " +
           "(m.tags LIKE CONCAT('%', :tag, '%'))")
    List<MomentEntity> findByTag(@Param("userId") UUID userId, @Param("tag") String tag);
    
    @Query("SELECT m FROM MomentEntity m WHERE m.userId = :userId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) AND " +
           "(m.tags LIKE CONCAT('%', :tag, '%'))")
    List<MomentEntity> searchByContentAndTag(@Param("userId") UUID userId, @Param("query") String query, @Param("tag") String tag);
    
    void deleteByUserId(UUID userId);
}