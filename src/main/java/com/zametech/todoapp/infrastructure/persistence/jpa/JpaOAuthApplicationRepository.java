package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.OAuthApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaOAuthApplicationRepository extends JpaRepository<OAuthApplication, UUID> {
    Optional<OAuthApplication> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}