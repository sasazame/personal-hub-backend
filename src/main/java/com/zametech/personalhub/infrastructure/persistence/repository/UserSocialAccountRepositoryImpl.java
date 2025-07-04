package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.model.UserSocialAccount;
import com.zametech.personalhub.domain.repository.UserSocialAccountRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import com.zametech.personalhub.infrastructure.persistence.entity.UserSocialAccountEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaUserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserSocialAccountRepositoryImpl implements UserSocialAccountRepository {
    
    private final JpaUserSocialAccountRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    
    @Override
    public Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(this::toModel);
    }
    
    @Override
    public Optional<UserSocialAccount> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toModel);
    }
    
    @Override
    public Optional<UserSocialAccount> findByUserIdAndProvider(UUID userId, String provider) {
        return jpaRepository.findByUserIdAndProvider(userId, provider)
                .map(this::toModel);
    }
    
    @Override
    public List<UserSocialAccount> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }
    
    @Override
    public UserSocialAccount save(UserSocialAccount socialAccount) {
        UserSocialAccountEntity entity = toEntity(socialAccount);
        UserSocialAccountEntity savedEntity = jpaRepository.save(entity);
        return toModel(savedEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void deleteByUserIdAndProvider(UUID userId, String provider) {
        jpaRepository.deleteByUserIdAndProvider(userId, provider);
    }
    
    @Override
    public boolean existsByProviderAndProviderUserId(String provider, String providerUserId) {
        return jpaRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }
    
    private UserSocialAccount toModel(UserSocialAccountEntity entity) {
        UserSocialAccount model = new UserSocialAccount();
        model.setId(entity.getId());
        model.setUser(toUserModel(entity.getUser()));
        model.setProvider(entity.getProvider());
        model.setProviderUserId(entity.getProviderUserId());
        model.setEmail(entity.getEmail());
        model.setEmailVerified(entity.getEmailVerified());
        model.setName(entity.getName());
        model.setGivenName(entity.getGivenName());
        model.setFamilyName(entity.getFamilyName());
        model.setPicture(entity.getPicture());
        model.setLocale(entity.getLocale());
        model.setProfileData(entity.getProfileData());
        model.setAccessTokenEncrypted(entity.getAccessTokenEncrypted());
        model.setRefreshTokenEncrypted(entity.getRefreshTokenEncrypted());
        model.setTokenExpiresAt(entity.getTokenExpiresAt());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }
    
    private UserSocialAccountEntity toEntity(UserSocialAccount model) {
        UserSocialAccountEntity entity = new UserSocialAccountEntity();
        entity.setId(model.getId());
        
        // Load the user entity from database
        UserEntity userEntity = userJpaRepository.findById(model.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + model.getUser().getId()));
        entity.setUser(userEntity);
        
        entity.setProvider(model.getProvider());
        entity.setProviderUserId(model.getProviderUserId());
        entity.setEmail(model.getEmail());
        entity.setEmailVerified(model.getEmailVerified());
        entity.setName(model.getName());
        entity.setGivenName(model.getGivenName());
        entity.setFamilyName(model.getFamilyName());
        entity.setPicture(model.getPicture());
        entity.setLocale(model.getLocale());
        entity.setProfileData(model.getProfileData());
        entity.setAccessTokenEncrypted(model.getAccessTokenEncrypted());
        entity.setRefreshTokenEncrypted(model.getRefreshTokenEncrypted());
        entity.setTokenExpiresAt(model.getTokenExpiresAt());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
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
}