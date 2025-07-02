package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.OAuthApplication;
import com.zametech.todoapp.infrastructure.persistence.entity.OAuthApplicationEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaOAuthApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthApplicationRepositoryImplTest {

    @Mock
    private JpaOAuthApplicationRepository jpaRepository;

    @InjectMocks
    private OAuthApplicationRepositoryImpl repository;

    private UUID applicationId;
    private OAuthApplicationEntity entity;
    private OAuthApplication model;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        entity = new OAuthApplicationEntity();
        entity.setId(applicationId);
        entity.setClientId("test-client-id");
        entity.setClientSecretHash("hashed-secret");
        entity.setRedirectUris(Arrays.asList("https://example.com/callback"));
        entity.setScopes(Arrays.asList("read", "write"));
        entity.setApplicationType("web");
        entity.setGrantTypes(Arrays.asList("authorization_code", "refresh_token"));
        entity.setResponseTypes(Arrays.asList("code"));
        entity.setTokenEndpointAuthMethod("client_secret_basic");
        entity.setApplicationName("Test App");
        entity.setApplicationUri("https://example.com");
        entity.setContacts(Arrays.asList("admin@example.com"));
        entity.setLogoUri("https://example.com/logo.png");
        entity.setTosUri("https://example.com/tos");
        entity.setPolicyUri("https://example.com/policy");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        model = OAuthApplication.builder()
                .id(applicationId)
                .clientId("test-client-id")
                .clientSecretHash("hashed-secret")
                .redirectUris(Arrays.asList("https://example.com/callback"))
                .scopes(Arrays.asList("read", "write"))
                .applicationType("web")
                .grantTypes(Arrays.asList("authorization_code", "refresh_token"))
                .responseTypes(Arrays.asList("code"))
                .tokenEndpointAuthMethod("client_secret_basic")
                .applicationName("Test App")
                .applicationUri("https://example.com")
                .contacts(Arrays.asList("admin@example.com"))
                .logoUri("https://example.com/logo.png")
                .tosUri("https://example.com/tos")
                .policyUri("https://example.com/policy")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void findByClientId_WhenExists_ReturnsApplication() {
        // Given
        when(jpaRepository.findByClientId("test-client-id")).thenReturn(Optional.of(entity));

        // When
        Optional<OAuthApplication> result = repository.findByClientId("test-client-id");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getClientId()).isEqualTo("test-client-id");
        assertThat(result.get().getApplicationName()).isEqualTo("Test App");
        assertThat(result.get().getRedirectUris()).containsExactly("https://example.com/callback");
        assertThat(result.get().getScopes()).containsExactly("read", "write");
    }

    @Test
    void findByClientId_WhenNotExists_ReturnsEmpty() {
        // Given
        when(jpaRepository.findByClientId("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<OAuthApplication> result = repository.findByClientId("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void save_CreatesNewApplication() {
        // Given
        when(jpaRepository.save(any(OAuthApplicationEntity.class))).thenReturn(entity);

        // When
        OAuthApplication result = repository.save(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(applicationId);
        assertThat(result.getClientId()).isEqualTo("test-client-id");
        verify(jpaRepository).save(any(OAuthApplicationEntity.class));
    }

    @Test
    void deleteById_DeletesApplication() {
        // When
        repository.deleteById(applicationId);

        // Then
        verify(jpaRepository).deleteById(applicationId);
    }

    @Test
    void existsByClientId_WhenExists_ReturnsTrue() {
        // Given
        when(jpaRepository.existsByClientId("test-client-id")).thenReturn(true);

        // When
        boolean result = repository.existsByClientId("test-client-id");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void existsByClientId_WhenNotExists_ReturnsFalse() {
        // Given
        when(jpaRepository.existsByClientId("non-existent")).thenReturn(false);

        // When
        boolean result = repository.existsByClientId("non-existent");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void toModel_ConvertsAllFields() {
        // Given
        when(jpaRepository.findByClientId("test-client-id")).thenReturn(Optional.of(entity));

        // When
        Optional<OAuthApplication> result = repository.findByClientId("test-client-id");

        // Then
        assertThat(result).isPresent();
        OAuthApplication app = result.get();
        assertThat(app.getId()).isEqualTo(entity.getId());
        assertThat(app.getClientId()).isEqualTo(entity.getClientId());
        assertThat(app.getClientSecretHash()).isEqualTo(entity.getClientSecretHash());
        assertThat(app.getRedirectUris()).isEqualTo(entity.getRedirectUris());
        assertThat(app.getScopes()).isEqualTo(entity.getScopes());
        assertThat(app.getApplicationType()).isEqualTo(entity.getApplicationType());
        assertThat(app.getGrantTypes()).isEqualTo(entity.getGrantTypes());
        assertThat(app.getResponseTypes()).isEqualTo(entity.getResponseTypes());
        assertThat(app.getTokenEndpointAuthMethod()).isEqualTo(entity.getTokenEndpointAuthMethod());
        assertThat(app.getApplicationName()).isEqualTo(entity.getApplicationName());
        assertThat(app.getApplicationUri()).isEqualTo(entity.getApplicationUri());
        assertThat(app.getContacts()).isEqualTo(entity.getContacts());
        assertThat(app.getLogoUri()).isEqualTo(entity.getLogoUri());
        assertThat(app.getTosUri()).isEqualTo(entity.getTosUri());
        assertThat(app.getPolicyUri()).isEqualTo(entity.getPolicyUri());
        assertThat(app.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(app.getUpdatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    void save_WithMinimalFields_Succeeds() {
        // Given
        OAuthApplication minimalApp = OAuthApplication.builder()
                .clientId("minimal-client")
                .clientSecretHash("minimal-secret")
                .redirectUris(Arrays.asList("https://minimal.com/callback"))
                .scopes(Arrays.asList("read"))
                .applicationType("web")
                .grantTypes(Arrays.asList("authorization_code"))
                .responseTypes(Arrays.asList("code"))
                .tokenEndpointAuthMethod("client_secret_basic")
                .applicationName("Minimal App")
                .build();
        
        OAuthApplicationEntity minimalEntity = new OAuthApplicationEntity();
        minimalEntity.setId(UUID.randomUUID());
        minimalEntity.setClientId("minimal-client");
        minimalEntity.setClientSecretHash("minimal-secret");
        minimalEntity.setRedirectUris(Arrays.asList("https://minimal.com/callback"));
        minimalEntity.setScopes(Arrays.asList("read"));
        minimalEntity.setApplicationType("web");
        minimalEntity.setGrantTypes(Arrays.asList("authorization_code"));
        minimalEntity.setResponseTypes(Arrays.asList("code"));
        minimalEntity.setTokenEndpointAuthMethod("client_secret_basic");
        minimalEntity.setApplicationName("Minimal App");
        
        when(jpaRepository.save(any(OAuthApplicationEntity.class))).thenReturn(minimalEntity);

        // When
        OAuthApplication result = repository.save(minimalApp);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo("minimal-client");
        assertThat(result.getApplicationName()).isEqualTo("Minimal App");
    }
}