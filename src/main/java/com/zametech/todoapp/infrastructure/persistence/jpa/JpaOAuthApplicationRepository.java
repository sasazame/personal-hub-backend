package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.infrastructure.persistence.entity.OAuthApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaOAuthApplicationRepository extends JpaRepository<OAuthApplicationEntity, UUID> {
    Optional<OAuthApplicationEntity> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}