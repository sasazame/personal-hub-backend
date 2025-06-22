package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    List<UserSocialAccount> findByUserId(UUID userId);
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}