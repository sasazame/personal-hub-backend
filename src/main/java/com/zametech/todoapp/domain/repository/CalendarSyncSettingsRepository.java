package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarSyncSettingsRepository extends JpaRepository<CalendarSyncSettingsEntity, Long> {
    
    List<CalendarSyncSettingsEntity> findByUserId(UUID userId);
    
    List<CalendarSyncSettingsEntity> findByUserIdAndSyncEnabledTrue(UUID userId);
    
    Optional<CalendarSyncSettingsEntity> findByUserIdAndGoogleCalendarId(UUID userId, String googleCalendarId);
    
    boolean existsByUserIdAndGoogleCalendarId(UUID userId, String googleCalendarId);
    
    void deleteByUserIdAndGoogleCalendarId(UUID userId, String googleCalendarId);
}