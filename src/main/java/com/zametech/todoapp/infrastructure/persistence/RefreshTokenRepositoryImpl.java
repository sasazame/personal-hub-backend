package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.RefreshToken;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.RefreshTokenRepository;
import com.zametech.todoapp.infrastructure.persistence.entity.RefreshTokenEntity;
import com.zametech.todoapp.infrastructure.persistence.entity.UserEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    
    private final JpaRefreshTokenRepository jpaRepository;
    
    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toModel);
    }
    
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = toEntity(refreshToken);
        RefreshTokenEntity savedEntity = jpaRepository.save(entity);
        return toModel(savedEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId, String clientId) {
        jpaRepository.revokeAllUserTokens(userId, clientId, LocalDateTime.now());
    }
    
    private RefreshToken toModel(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .tokenHash(entity.getTokenHash())
                .user(entity.getUser() != null ? toUserModel(entity.getUser()) : null)
                .clientId(entity.getClientId())
                .scopes(entity.getScopes())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.getRevoked())
                .revokedAt(entity.getRevokedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private RefreshTokenEntity toEntity(RefreshToken model) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(model.getId());
        entity.setTokenHash(model.getTokenHash());
        entity.setUser(model.getUser() != null ? toUserEntity(model.getUser()) : null);
        entity.setClientId(model.getClientId());
        entity.setScopes(model.getScopes());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setRevoked(model.getRevoked());
        entity.setRevokedAt(model.getRevokedAt());
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