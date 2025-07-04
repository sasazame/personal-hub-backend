package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
    Optional<PasswordResetTokenEntity> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}