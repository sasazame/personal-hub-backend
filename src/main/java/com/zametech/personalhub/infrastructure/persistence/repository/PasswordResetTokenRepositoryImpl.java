package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.PasswordResetToken;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.PasswordResetTokenRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;

    @Override
    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = toEntity(token);
        PasswordResetTokenEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        PasswordResetToken domain = new PasswordResetToken();
        domain.setId(entity.getId());
        domain.setToken(entity.getToken());
        domain.setExpiresAt(entity.getExpiresAt());
        domain.setUsed(entity.isUsed());
        domain.setCreatedAt(entity.getCreatedAt());
        
        User user = new User();
        UserEntity userEntity = entity.getUser();
        user.setId(userEntity.getId());
        user.setEmail(userEntity.getEmail());
        user.setPassword(userEntity.getPassword());
        user.setUsername(userEntity.getUsername());
        user.setEnabled(userEntity.isEnabled());
        user.setEmailVerified(userEntity.getEmailVerified());
        user.setProfilePictureUrl(userEntity.getProfilePictureUrl());
        user.setGivenName(userEntity.getGivenName());
        user.setFamilyName(userEntity.getFamilyName());
        user.setLocale(userEntity.getLocale());
        user.setWeekStartDay(userEntity.getWeekStartDay());
        user.setCreatedAt(userEntity.getCreatedAt());
        user.setUpdatedAt(userEntity.getUpdatedAt());
        
        domain.setUser(user);
        return domain;
    }

    private PasswordResetTokenEntity toEntity(PasswordResetToken domain) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setId(domain.getId());
        entity.setToken(domain.getToken());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsed(domain.isUsed());
        entity.setCreatedAt(domain.getCreatedAt());
        
        if (domain.getUser() != null) {
            UserEntity userEntity = new UserEntity();
            userEntity.setId(domain.getUser().getId());
            entity.setUser(userEntity);
        }
        
        return entity;
    }
}