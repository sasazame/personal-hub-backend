package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.UserSocialAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSocialAccountRepository {
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    List<UserSocialAccount> findByUserId(UUID userId);
    UserSocialAccount save(UserSocialAccount socialAccount);
    void deleteById(UUID id);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}