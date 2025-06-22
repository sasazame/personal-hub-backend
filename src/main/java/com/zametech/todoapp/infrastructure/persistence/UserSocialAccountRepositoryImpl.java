package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaUserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public boolean existsByProviderAndProviderUserId(String provider, String providerUserId) {
        return jpaRepository.existsByProviderAndProviderUserId(provider, providerUserId);
    }
}