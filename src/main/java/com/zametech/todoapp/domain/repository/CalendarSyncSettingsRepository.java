package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.infrastructure.persistence.entity.CalendarSyncSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarSyncSettingsRepository extends JpaRepository<CalendarSyncSettingsEntity, Long> {
    
    List<CalendarSyncSettingsEntity> findByUserId(Long userId);
    
    List<CalendarSyncSettingsEntity> findByUserIdAndSyncEnabledTrue(Long userId);
    
    Optional<CalendarSyncSettingsEntity> findByUserIdAndGoogleCalendarId(Long userId, String googleCalendarId);
    
    boolean existsByUserIdAndGoogleCalendarId(Long userId, String googleCalendarId);
    
    void deleteByUserIdAndGoogleCalendarId(Long userId, String googleCalendarId);
}