package com.zametech.todoapp.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthStateServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private OAuthStateService oAuthStateService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache("oauthStates")).thenReturn(cache);
    }

    @Test
    void generateState_Success() {
        // Given
        String provider = "google";

        // When
        String state = oAuthStateService.generateState(provider);

        // Then
        assertThat(state).isNotNull();
        assertThat(state).hasSize(36); // UUID format
        verify(cache).put(eq("oauth_state:" + state), eq(provider));
    }

    @Test
    void generateState_WhenCacheNotFound_ThrowsException() {
        // Given
        when(cacheManager.getCache("oauthStates")).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> oAuthStateService.generateState("google"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("OAuth state cache is not configured");
    }

    @Test
    void validateStateAndGetProvider_ValidState_ReturnsProvider() {
        // Given
        String state = "test-state";
        String provider = "github";
        when(cache.get("oauth_state:" + state, String.class)).thenReturn(provider);

        // When
        String result = oAuthStateService.validateStateAndGetProvider(state);

        // Then
        assertThat(result).isEqualTo(provider);
        verify(cache).evict("oauth_state:" + state);
    }

    @Test
    void validateStateAndGetProvider_NullState_ReturnsNull() {
        // When
        String result = oAuthStateService.validateStateAndGetProvider(null);

        // Then
        assertThat(result).isNull();
        verify(cache, never()).get(any(), any(Class.class));
    }

    @Test
    void validateStateAndGetProvider_EmptyState_ReturnsNull() {
        // When
        String result = oAuthStateService.validateStateAndGetProvider("");

        // Then
        assertThat(result).isNull();
        verify(cache, never()).get(any(), any(Class.class));
    }

    @Test
    void validateStateAndGetProvider_StateNotInCache_ReturnsNull() {
        // Given
        String state = "invalid-state";
        when(cache.get("oauth_state:" + state, String.class)).thenReturn(null);

        // When
        String result = oAuthStateService.validateStateAndGetProvider(state);

        // Then
        assertThat(result).isNull();
        verify(cache, never()).evict(any());
    }

    @Test
    void validateStateAndGetProvider_CacheNotFound_ReturnsNull() {
        // Given
        when(cacheManager.getCache("oauthStates")).thenReturn(null);

        // When
        String result = oAuthStateService.validateStateAndGetProvider("test-state");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void invalidateState_ValidState_RemovesFromCache() {
        // Given
        String state = "test-state";

        // When
        oAuthStateService.invalidateState(state);

        // Then
        verify(cache).evict("oauth_state:" + state);
    }

    @Test
    void invalidateState_NullState_DoesNothing() {
        // When
        oAuthStateService.invalidateState(null);

        // Then
        verify(cache, never()).evict(any());
    }

    @Test
    void invalidateState_EmptyState_DoesNothing() {
        // When
        oAuthStateService.invalidateState("");

        // Then
        verify(cache, never()).evict(any());
    }

    @Test
    void invalidateState_CacheNotFound_DoesNotThrow() {
        // Given
        when(cacheManager.getCache("oauthStates")).thenReturn(null);

        // When/Then - should not throw
        oAuthStateService.invalidateState("test-state");
    }
}