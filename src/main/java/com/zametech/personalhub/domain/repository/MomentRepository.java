package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MomentRepository {
    Moment save(Moment moment);
    Optional<Moment> findById(Long id);
    Optional<Moment> findByIdAndUserId(Long id, UUID userId);
    Page<Moment> findByUserId(UUID userId, Pageable pageable);
    Page<Moment> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Moment> searchByContent(UUID userId, String query);
    List<Moment> findByTag(UUID userId, String tag);
    List<Moment> searchByContentAndTag(UUID userId, String query, String tag);
    void deleteById(Long id);
    void deleteByUserId(UUID userId);
}