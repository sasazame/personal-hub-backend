package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.infrastructure.persistence.entity.UserSocialAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserSocialAccountRepository extends JpaRepository<UserSocialAccountEntity, UUID> {
    Optional<UserSocialAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    @Query("SELECT usa FROM UserSocialAccountEntity usa WHERE usa.user.id = :userId AND usa.provider = :provider")
    Optional<UserSocialAccountEntity> findByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
    
    List<UserSocialAccountEntity> findByUserId(UUID userId);
    
    @Modifying
    @Query("DELETE FROM UserSocialAccountEntity usa WHERE usa.user.id = :userId AND usa.provider = :provider")
    void deleteByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
    
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}