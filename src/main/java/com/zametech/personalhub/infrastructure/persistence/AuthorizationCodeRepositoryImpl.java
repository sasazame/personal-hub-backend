package com.zametech.personalhub.infrastructure.persistence;

import com.zametech.personalhub.domain.model.AuthorizationCode;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.AuthorizationCodeRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.AuthorizationCodeEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaAuthorizationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorizationCodeRepositoryImpl implements AuthorizationCodeRepository {
    
    private final JpaAuthorizationCodeRepository jpaRepository;
    
    @Override
    public Optional<AuthorizationCode> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toModel);
    }
    
    @Override
    public AuthorizationCode save(AuthorizationCode authorizationCode) {
        AuthorizationCodeEntity entity = toEntity(authorizationCode);
        AuthorizationCodeEntity savedEntity = jpaRepository.save(entity);
        return toModel(savedEntity);
    }
    
    @Override
    public void deleteByCode(String code) {
        jpaRepository.deleteByCode(code);
    }
    
    @Override
    @Transactional
    public void deleteExpiredCodes() {
        jpaRepository.deleteExpiredCodes(LocalDateTime.now());
    }
    
    private AuthorizationCode toModel(AuthorizationCodeEntity entity) {
        return AuthorizationCode.builder()
                .code(entity.getCode())
                .clientId(entity.getClientId())
                .user(entity.getUser() != null ? toUserModel(entity.getUser()) : null)
                .redirectUri(entity.getRedirectUri())
                .scopes(entity.getScopes())
                .codeChallenge(entity.getCodeChallenge())
                .codeChallengeMethod(entity.getCodeChallengeMethod())
                .nonce(entity.getNonce())
                .state(entity.getState())
                .authTime(entity.getAuthTime())
                .expiresAt(entity.getExpiresAt())
                .used(entity.getUsed())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private AuthorizationCodeEntity toEntity(AuthorizationCode model) {
        AuthorizationCodeEntity entity = new AuthorizationCodeEntity();
        entity.setCode(model.getCode());
        entity.setClientId(model.getClientId());
        entity.setUser(model.getUser() != null ? toUserEntity(model.getUser()) : null);
        entity.setRedirectUri(model.getRedirectUri());
        entity.setScopes(model.getScopes());
        entity.setCodeChallenge(model.getCodeChallenge());
        entity.setCodeChallengeMethod(model.getCodeChallengeMethod());
        entity.setNonce(model.getNonce());
        entity.setState(model.getState());
        entity.setAuthTime(model.getAuthTime());
        entity.setExpiresAt(model.getExpiresAt());
        entity.setUsed(model.getUsed());
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