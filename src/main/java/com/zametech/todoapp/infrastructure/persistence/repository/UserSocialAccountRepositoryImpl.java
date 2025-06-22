package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaUserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserSocialAccountRepositoryImpl implements UserSocialAccountRepository {
    
    private final JpaUserSocialAccountRepository jpaRepository;
    
    @Override
    public Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }
    
    @Override
    public Optional<UserSocialAccount> findById(UUID id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Optional<UserSocialAccount> findByUserIdAndProvider(UUID userId, String provider) {
        return jpaRepository.findByUserIdAndProvider(userId, provider);
    }
    
    @Override
    public List<UserSocialAccount> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }
    
    @Override
    public UserSocialAccount save(UserSocialAccount socialAccount) {
        return jpaRepository.save(socialAccount);
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
}