package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.SecurityEvent;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.SecurityEventRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.SecurityEventEntity;
import com.zametech.todoapp.infrastructure.persistence.entity.UserEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaSecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SecurityEventRepositoryImpl implements SecurityEventRepository {
    
    private final JpaSecurityEventRepository jpaRepository;
    
    @Override
    public SecurityEvent save(SecurityEvent event) {
        SecurityEventEntity entity = toEntity(event);
        SecurityEventEntity savedEntity = jpaRepository.save(entity);
        return toModel(savedEntity);
    }
    
    @Override
    public List<SecurityEvent> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after) {
        return jpaRepository.findByUserIdAndCreatedAtAfter(userId, after).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SecurityEvent> findByEventTypeAndCreatedAtAfter(SecurityEvent.EventType eventType, LocalDateTime after) {
        return jpaRepository.findByEventTypeAndCreatedAtAfter(eventType, after).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countFailedLoginAttempts(UUID userId, LocalDateTime after) {
        return jpaRepository.countFailedLoginAttempts(userId, after);
    }
    
    @Override
    public List<SecurityEvent> findRecentEventsByUser(UUID userId, int limit) {
        return jpaRepository.findTopByUserIdOrderByCreatedAtDesc(userId, limit).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByEventTypeAndSuccessAndCreatedAtAfter(SecurityEvent.EventType eventType, boolean success, LocalDateTime after) {
        return jpaRepository.countByEventTypeAndSuccessAndCreatedAtAfter(eventType, success, after);
    }
    
    private SecurityEvent toModel(SecurityEventEntity entity) {
        return SecurityEvent.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .user(entity.getUser() != null ? toUserModel(entity.getUser()) : null)
                .clientId(entity.getClientId())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .success(entity.getSuccess())
                .errorCode(entity.getErrorCode())
                .errorDescription(entity.getErrorDescription())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private SecurityEventEntity toEntity(SecurityEvent model) {
        SecurityEventEntity entity = new SecurityEventEntity();
        entity.setId(model.getId());
        entity.setEventType(model.getEventType());
        entity.setUser(model.getUser() != null ? toUserEntity(model.getUser()) : null);
        entity.setClientId(model.getClientId());
        entity.setIpAddress(model.getIpAddress());
        entity.setUserAgent(model.getUserAgent());
        entity.setSuccess(model.getSuccess());
        entity.setErrorCode(model.getErrorCode());
        entity.setErrorDescription(model.getErrorDescription());
        entity.setMetadata(model.getMetadata());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }
    
    private User toUserModel(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPassword(entity.getPassword());
        user.setUsername(entity.getUsername());
        user.setEnabled(entity.isEnabled());
        user.setEmailVerified(entity.getEmailVerified());
        user.setProfilePictureUrl(entity.getProfilePictureUrl());
        user.setGivenName(entity.getGivenName());
        user.setFamilyName(entity.getFamilyName());
        user.setLocale(entity.getLocale());
        user.setWeekStartDay(entity.getWeekStartDay());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }
    
    private UserEntity toUserEntity(User model) {
        UserEntity entity = new UserEntity();
        entity.setId(model.getId());
        // Only set the ID for reference, as we don't want to cascade changes to the user
        return entity;
    }
}