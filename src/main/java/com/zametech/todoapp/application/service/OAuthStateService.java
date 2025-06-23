package com.zametech.todoapp.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth state parameter management service
 * Stores and validates state parameters for CSRF protection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthStateService {
    
    private static final String CACHE_NAME = "oauthStates";
    private static final long STATE_TTL_MINUTES = 10;
    
    private final CacheManager cacheManager;
    
    /**
     * Generate and store a new state parameter
     */
    public String generateState(String provider) {
        String state = UUID.randomUUID().toString();
        String cacheKey = buildCacheKey(state);
        
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.put(cacheKey, provider);
            log.debug("Generated OAuth state: {} for provider: {}", state, provider);
        } else {
            log.error("OAuth state cache not found");
            throw new IllegalStateException("OAuth state cache is not configured");
        }
        
        return state;
    }
    
    /**
     * Validate state parameter and return provider if valid
     */
    public String validateStateAndGetProvider(String state) {
        if (state == null || state.trim().isEmpty()) {
            log.warn("Invalid OAuth state: null or empty");
            return null;
        }
        
        String cacheKey = buildCacheKey(state);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        
        if (cache == null) {
            log.error("OAuth state cache not found");
            return null;
        }
        
        String provider = cache.get(cacheKey, String.class);
        
        if (provider != null) {
            // Remove state after validation (one-time use)
            cache.evict(cacheKey);
            log.debug("Validated OAuth state: {} for provider: {}", state, provider);
        } else {
            log.warn("OAuth state not found or expired: {}", state);
        }
        
        return provider;
    }
    
    /**
     * Invalidate a state parameter
     */
    public void invalidateState(String state) {
        if (state == null || state.trim().isEmpty()) {
            return;
        }
        
        String cacheKey = buildCacheKey(state);
        Cache cache = cacheManager.getCache(CACHE_NAME);
        
        if (cache != null) {
            cache.evict(cacheKey);
            log.debug("Invalidated OAuth state: {}", state);
        }
    }
    
    private String buildCacheKey(String state) {
        return "oauth_state:" + state;
    }
}