package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken refreshToken);
    void deleteById(UUID id);
    void deleteExpiredTokens();
    void revokeAllUserTokens(UUID userId, String clientId);
}