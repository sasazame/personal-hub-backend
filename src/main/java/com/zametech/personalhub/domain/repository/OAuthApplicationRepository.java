package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.OAuthApplication;
import java.util.Optional;
import java.util.UUID;

public interface OAuthApplicationRepository {
    Optional<OAuthApplication> findByClientId(String clientId);
    OAuthApplication save(OAuthApplication application);
    void deleteById(UUID id);
    boolean existsByClientId(String clientId);
}