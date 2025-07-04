package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.UserSocialAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSocialAccountRepository {
    
    UserSocialAccount save(UserSocialAccount socialAccount);
    
    Optional<UserSocialAccount> findById(UUID id);
    
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    Optional<UserSocialAccount> findByUserIdAndProvider(UUID userId, String provider);
    
    List<UserSocialAccount> findByUserId(UUID userId);
    
    void deleteById(UUID id);
    
    void deleteByUserIdAndProvider(UUID userId, String provider);
    
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}