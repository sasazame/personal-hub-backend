package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.RefreshToken;
import com.zametech.todoapp.domain.repository.RefreshTokenRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    
    private final JpaRefreshTokenRepository jpaRepository;
    
    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash);
    }
    
    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId, String clientId) {
        jpaRepository.revokeAllUserTokens(userId, clientId, LocalDateTime.now());
    }
}