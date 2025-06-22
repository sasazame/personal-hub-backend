package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.OAuthApplication;
import com.zametech.todoapp.domain.repository.OAuthApplicationRepository;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaOAuthApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OAuthApplicationRepositoryImpl implements OAuthApplicationRepository {
    
    private final JpaOAuthApplicationRepository jpaRepository;
    
    @Override
    public Optional<OAuthApplication> findByClientId(String clientId) {
        return jpaRepository.findByClientId(clientId);
    }
    
    @Override
    public OAuthApplication save(OAuthApplication application) {
        return jpaRepository.save(application);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByClientId(String clientId) {
        return jpaRepository.existsByClientId(clientId);
    }
}