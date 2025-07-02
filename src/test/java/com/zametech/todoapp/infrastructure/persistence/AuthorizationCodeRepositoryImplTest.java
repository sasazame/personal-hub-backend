package com.zametech.todoapp.infrastructure.persistence;

import com.zametech.todoapp.domain.model.AuthorizationCode;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.infrastructure.persistence.entity.AuthorizationCodeEntity;
import com.zametech.todoapp.infrastructure.persistence.entity.UserEntity;
import com.zametech.todoapp.infrastructure.persistence.jpa.JpaAuthorizationCodeRepository;
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
class AuthorizationCodeRepositoryImplTest {

    @Mock
    private JpaAuthorizationCodeRepository jpaRepository;

    @InjectMocks
    private AuthorizationCodeRepositoryImpl repository;

    private AuthorizationCodeEntity entity;
    private AuthorizationCode model;
    private UserEntity userEntity;
    private User user;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setUsername("testuser");
        userEntity.setEnabled(true);
        userEntity.setEmailVerified(true);
        userEntity.setCreatedAt(now);
        userEntity.setUpdatedAt(now);
        
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        
        entity = new AuthorizationCodeEntity();
        entity.setCode("test-auth-code");
        entity.setClientId("test-client-id");
        entity.setUser(userEntity);
        entity.setRedirectUri("https://example.com/callback");
        entity.setScopes(Arrays.asList("openid", "profile", "email"));
        entity.setCodeChallenge("challenge-value");
        entity.setCodeChallengeMethod("S256");
        entity.setNonce("nonce-value");
        entity.setState("state-value");
        entity.setAuthTime(now);
        entity.setExpiresAt(now.plusMinutes(10));
        entity.setUsed(false);
        entity.setCreatedAt(now);
        
        model = AuthorizationCode.builder()
                .code("test-auth-code")
                .clientId("test-client-id")
                .user(user)
                .redirectUri("https://example.com/callback")
                .scopes(Arrays.asList("openid", "profile", "email"))
                .codeChallenge("challenge-value")
                .codeChallengeMethod("S256")
                .nonce("nonce-value")
                .state("state-value")
                .authTime(now)
                .expiresAt(now.plusMinutes(10))
                .used(false)
                .createdAt(now)
                .build();
    }

    @Test
    void findByCode_WhenExists_ReturnsAuthorizationCode() {
        // Given
        when(jpaRepository.findByCode("test-auth-code")).thenReturn(Optional.of(entity));

        // When
        Optional<AuthorizationCode> result = repository.findByCode("test-auth-code");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("test-auth-code");
        assertThat(result.get().getClientId()).isEqualTo("test-client-id");
        assertThat(result.get().getScopes()).containsExactly("openid", "profile", "email");
        assertThat(result.get().getUser()).isNotNull();
        assertThat(result.get().getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByCode_WhenNotExists_ReturnsEmpty() {
        // Given
        when(jpaRepository.findByCode("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<AuthorizationCode> result = repository.findByCode("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void save_CreatesNewAuthorizationCode() {
        // Given
        when(jpaRepository.save(any(AuthorizationCodeEntity.class))).thenReturn(entity);

        // When
        AuthorizationCode result = repository.save(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("test-auth-code");
        assertThat(result.getClientId()).isEqualTo("test-client-id");
        verify(jpaRepository).save(any(AuthorizationCodeEntity.class));
    }

    @Test
    void deleteByCode_DeletesAuthorizationCode() {
        // When
        repository.deleteByCode("test-auth-code");

        // Then
        verify(jpaRepository).deleteByCode("test-auth-code");
    }

    @Test
    void deleteExpiredCodes_DeletesExpiredCodes() {
        // When
        repository.deleteExpiredCodes();

        // Then
        verify(jpaRepository).deleteExpiredCodes(any(LocalDateTime.class));
    }

    @Test
    void toModel_WithNullUser_HandlesGracefully() {
        // Given
        entity.setUser(null);
        when(jpaRepository.findByCode("test-auth-code")).thenReturn(Optional.of(entity));

        // When
        Optional<AuthorizationCode> result = repository.findByCode("test-auth-code");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isNull();
    }

    @Test
    void toModel_ConvertsAllFields() {
        // Given
        when(jpaRepository.findByCode("test-auth-code")).thenReturn(Optional.of(entity));

        // When
        Optional<AuthorizationCode> result = repository.findByCode("test-auth-code");

        // Then
        assertThat(result).isPresent();
        AuthorizationCode code = result.get();
        assertThat(code.getCode()).isEqualTo(entity.getCode());
        assertThat(code.getClientId()).isEqualTo(entity.getClientId());
        assertThat(code.getRedirectUri()).isEqualTo(entity.getRedirectUri());
        assertThat(code.getScopes()).isEqualTo(entity.getScopes());
        assertThat(code.getCodeChallenge()).isEqualTo(entity.getCodeChallenge());
        assertThat(code.getCodeChallengeMethod()).isEqualTo(entity.getCodeChallengeMethod());
        assertThat(code.getNonce()).isEqualTo(entity.getNonce());
        assertThat(code.getState()).isEqualTo(entity.getState());
        assertThat(code.getAuthTime()).isEqualTo(entity.getAuthTime());
        assertThat(code.getExpiresAt()).isEqualTo(entity.getExpiresAt());
        assertThat(code.getUsed()).isEqualTo(entity.getUsed());
        assertThat(code.getCreatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void save_WithUsedCode_Succeeds() {
        // Given
        model = AuthorizationCode.builder()
                .code("used-auth-code")
                .clientId("test-client-id")
                .user(user)
                .redirectUri("https://example.com/callback")
                .scopes(Arrays.asList("openid"))
                .authTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        AuthorizationCodeEntity usedEntity = new AuthorizationCodeEntity();
        usedEntity.setCode("used-auth-code");
        usedEntity.setClientId("test-client-id");
        usedEntity.setUser(userEntity);
        usedEntity.setRedirectUri("https://example.com/callback");
        usedEntity.setScopes(Arrays.asList("openid"));
        usedEntity.setUsed(true);
        
        when(jpaRepository.save(any(AuthorizationCodeEntity.class))).thenReturn(usedEntity);

        // When
        AuthorizationCode result = repository.save(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsed()).isTrue();
    }

    @Test
    void toUserModel_ConvertsAllUserFields() {
        // Given
        userEntity.setProfilePictureUrl("https://example.com/avatar.jpg");
        userEntity.setGivenName("Test");
        userEntity.setFamilyName("User");
        userEntity.setLocale("en-US");
        userEntity.setWeekStartDay(1);
        
        when(jpaRepository.findByCode("test-auth-code")).thenReturn(Optional.of(entity));

        // When
        Optional<AuthorizationCode> result = repository.findByCode("test-auth-code");

        // Then
        assertThat(result).isPresent();
        User resultUser = result.get().getUser();
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getId()).isEqualTo(userEntity.getId());
        assertThat(resultUser.getEmail()).isEqualTo(userEntity.getEmail());
        assertThat(resultUser.getUsername()).isEqualTo(userEntity.getUsername());
        assertThat(resultUser.isEnabled()).isEqualTo(userEntity.isEnabled());
        assertThat(resultUser.getEmailVerified()).isEqualTo(userEntity.getEmailVerified());
        assertThat(resultUser.getProfilePictureUrl()).isEqualTo(userEntity.getProfilePictureUrl());
        assertThat(resultUser.getGivenName()).isEqualTo(userEntity.getGivenName());
        assertThat(resultUser.getFamilyName()).isEqualTo(userEntity.getFamilyName());
        assertThat(resultUser.getLocale()).isEqualTo(userEntity.getLocale());
        assertThat(resultUser.getWeekStartDay()).isEqualTo(userEntity.getWeekStartDay());
    }

    @Test
    void save_WithNullUser_HandlesGracefully() {
        // Given
        model = AuthorizationCode.builder()
                .code("no-user-auth-code")
                .clientId("test-client-id")
                .user(null)
                .redirectUri("https://example.com/callback")
                .scopes(Arrays.asList("openid"))
                .authTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        AuthorizationCodeEntity noUserEntity = new AuthorizationCodeEntity();
        noUserEntity.setCode("no-user-auth-code");
        noUserEntity.setClientId("test-client-id");
        noUserEntity.setUser(null);
        
        when(jpaRepository.save(any(AuthorizationCodeEntity.class))).thenReturn(noUserEntity);

        // When
        AuthorizationCode result = repository.save(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNull();
    }
}