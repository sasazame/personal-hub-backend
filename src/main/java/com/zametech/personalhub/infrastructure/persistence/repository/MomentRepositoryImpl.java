package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.Moment;
import com.zametech.personalhub.domain.repository.MomentRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.MomentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MomentRepositoryImpl implements MomentRepository {

    private final MomentJpaRepository momentJpaRepository;

    @Override
    public Moment save(Moment moment) {
        MomentEntity entity = toEntity(moment);
        MomentEntity savedEntity = momentJpaRepository.save(entity);
        return toModel(savedEntity);
    }

    @Override
    public Optional<Moment> findById(Long id) {
        return momentJpaRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    public Optional<Moment> findByIdAndUserId(Long id, UUID userId) {
        return momentJpaRepository.findByIdAndUserId(id, userId)
                .map(this::toModel);
    }

    @Override
    public Page<Moment> findByUserId(UUID userId, Pageable pageable) {
        return momentJpaRepository.findByUserId(userId, pageable)
                .map(this::toModel);
    }

    @Override
    public Page<Moment> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return momentJpaRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageable)
                .map(this::toModel);
    }

    @Override
    public List<Moment> searchByContent(UUID userId, String query) {
        return momentJpaRepository.searchByContent(userId, query)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<Moment> findByTag(UUID userId, String tag) {
        return momentJpaRepository.findByTag(userId, tag)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<Moment> searchByContentAndTag(UUID userId, String query, String tag) {
        return momentJpaRepository.searchByContentAndTag(userId, query, tag)
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        momentJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        momentJpaRepository.deleteByUserId(userId);
    }

    private MomentEntity toEntity(Moment moment) {
        MomentEntity entity = new MomentEntity();
        entity.setId(moment.getId());
        entity.setContent(moment.getContent());
        entity.setTags(moment.getTags());
        entity.setUserId(moment.getUserId());
        entity.setCreatedAt(moment.getCreatedAt());
        entity.setUpdatedAt(moment.getUpdatedAt());
        return entity;
    }

    private Moment toModel(MomentEntity entity) {
        return new Moment(
                entity.getId(),
                entity.getContent(),
                entity.getTags(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}