package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.AuthorizationCode;
import com.zametech.todoapp.domain.repository.AuthorizationCodeRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaAuthorizationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorizationCodeRepositoryImpl implements AuthorizationCodeRepository {
    
    private final JpaAuthorizationCodeRepository jpaRepository;
    
    @Override
    public Optional<AuthorizationCode> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }
    
    @Override
    public AuthorizationCode save(AuthorizationCode authorizationCode) {
        return jpaRepository.save(authorizationCode);
    }
    
    @Override
    public void deleteByCode(String code) {
        jpaRepository.deleteByCode(code);
    }
    
    @Override
    @Transactional
    public void deleteExpiredCodes() {
        jpaRepository.deleteExpiredCodes(LocalDateTime.now());
    }
}