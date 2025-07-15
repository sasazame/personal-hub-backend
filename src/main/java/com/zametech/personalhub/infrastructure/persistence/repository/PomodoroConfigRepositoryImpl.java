package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.PomodoroConfig;
import com.zametech.personalhub.domain.repository.PomodoroConfigRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.PomodoroConfigEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaPomodoroConfigRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of PomodoroConfigRepository using JPA.
 */
@Repository
@Transactional
public class PomodoroConfigRepositoryImpl implements PomodoroConfigRepository {
    private final JpaPomodoroConfigRepository jpaRepository;

    public PomodoroConfigRepositoryImpl(JpaPomodoroConfigRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PomodoroConfig save(PomodoroConfig config) {
        PomodoroConfigEntity entity = toEntity(config);
        PomodoroConfigEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<PomodoroConfig> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }

    private PomodoroConfig toDomain(PomodoroConfigEntity entity) {
        PomodoroConfig config = new PomodoroConfig();
        config.setId(entity.getId());
        config.setUserId(entity.getUserId());
        config.setWorkDuration(entity.getWorkDuration());
        config.setShortBreakDuration(entity.getShortBreakDuration());
        config.setLongBreakDuration(entity.getLongBreakDuration());
        config.setCyclesBeforeLongBreak(entity.getCyclesBeforeLongBreak());
        config.setAlarmSound(entity.getAlarmSound());
        config.setAlarmVolume(entity.getAlarmVolume());
        config.setAutoStartBreaks(entity.getAutoStartBreaks());
        config.setAutoStartWork(entity.getAutoStartWork());
        config.setCreatedAt(entity.getCreatedAt());
        config.setUpdatedAt(entity.getUpdatedAt());
        return config;
    }

    private PomodoroConfigEntity toEntity(PomodoroConfig config) {
        PomodoroConfigEntity entity = new PomodoroConfigEntity();
        if (config.getId() != null) {
            entity.setId(config.getId());
        }
        entity.setUserId(config.getUserId());
        entity.setWorkDuration(config.getWorkDuration());
        entity.setShortBreakDuration(config.getShortBreakDuration());
        entity.setLongBreakDuration(config.getLongBreakDuration());
        entity.setCyclesBeforeLongBreak(config.getCyclesBeforeLongBreak());
        entity.setAlarmSound(config.getAlarmSound());
        entity.setAlarmVolume(config.getAlarmVolume());
        entity.setAutoStartBreaks(config.getAutoStartBreaks());
        entity.setAutoStartWork(config.getAutoStartWork());
        entity.setCreatedAt(config.getCreatedAt());
        entity.setUpdatedAt(config.getUpdatedAt());
        return entity;
    }
}