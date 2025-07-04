package com.zametech.personalhub.infrastructure.persistence;

import com.zametech.personalhub.domain.model.OAuthApplication;
import com.zametech.personalhub.domain.repository.OAuthApplicationRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.OAuthApplicationEntity;
import com.zametech.personalhub.infrastructure.persistence.jpa.JpaOAuthApplicationRepository;
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
        return jpaRepository.findByClientId(clientId).map(this::toModel);
    }
    
    @Override
    public OAuthApplication save(OAuthApplication application) {
        OAuthApplicationEntity entity = toEntity(application);
        OAuthApplicationEntity savedEntity = jpaRepository.save(entity);
        return toModel(savedEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByClientId(String clientId) {
        return jpaRepository.existsByClientId(clientId);
    }
    
    private OAuthApplication toModel(OAuthApplicationEntity entity) {
        return OAuthApplication.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientSecretHash(entity.getClientSecretHash())
                .redirectUris(entity.getRedirectUris())
                .scopes(entity.getScopes())
                .applicationType(entity.getApplicationType())
                .grantTypes(entity.getGrantTypes())
                .responseTypes(entity.getResponseTypes())
                .tokenEndpointAuthMethod(entity.getTokenEndpointAuthMethod())
                .applicationName(entity.getApplicationName())
                .applicationUri(entity.getApplicationUri())
                .contacts(entity.getContacts())
                .logoUri(entity.getLogoUri())
                .tosUri(entity.getTosUri())
                .policyUri(entity.getPolicyUri())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private OAuthApplicationEntity toEntity(OAuthApplication model) {
        OAuthApplicationEntity entity = new OAuthApplicationEntity();
        entity.setId(model.getId());
        entity.setClientId(model.getClientId());
        entity.setClientSecretHash(model.getClientSecretHash());
        entity.setRedirectUris(model.getRedirectUris());
        entity.setScopes(model.getScopes());
        entity.setApplicationType(model.getApplicationType());
        entity.setGrantTypes(model.getGrantTypes());
        entity.setResponseTypes(model.getResponseTypes());
        entity.setTokenEndpointAuthMethod(model.getTokenEndpointAuthMethod());
        entity.setApplicationName(model.getApplicationName());
        entity.setApplicationUri(model.getApplicationUri());
        entity.setContacts(model.getContacts());
        entity.setLogoUri(model.getLogoUri());
        entity.setTosUri(model.getTosUri());
        entity.setPolicyUri(model.getPolicyUri());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }
}