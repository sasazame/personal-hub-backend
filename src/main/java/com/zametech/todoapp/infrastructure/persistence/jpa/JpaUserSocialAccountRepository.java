package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {
    Optional<UserSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    @Query("SELECT usa FROM UserSocialAccount usa WHERE usa.user.id = :userId AND usa.provider = :provider")
    Optional<UserSocialAccount> findByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
    
    List<UserSocialAccount> findByUserId(UUID userId);
    
    @Modifying
    @Query("DELETE FROM UserSocialAccount usa WHERE usa.user.id = :userId AND usa.provider = :provider")
    void deleteByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
    
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}