package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findById(UUID id);
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    void deleteExpiredTokens();
}