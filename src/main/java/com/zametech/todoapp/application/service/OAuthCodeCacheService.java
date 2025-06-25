package com.zametech.todoapp.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OAuth authorization code cache service
 * Prevents duplicate usage of authorization codes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthCodeCacheService {
    
    private static final String CACHE_NAME = "oauthCodes";
    private static final long CODE_TTL_SECONDS = 60; // 1 minute TTL
    
    // In-memory cache for authorization codes and their token responses
    private final Map<String, TokenCacheEntry> codeCache = new ConcurrentHashMap<>();
    
    /**
     * Store a successful token exchange result
     */
    public void cacheTokenResponse(String code, Map<String, Object> tokenResponse) {
        String cacheKey = buildCacheKey(code);
        TokenCacheEntry entry = new TokenCacheEntry(tokenResponse, System.currentTimeMillis());
        codeCache.put(cacheKey, entry);
        log.debug("Cached token response for code: {}", code.substring(0, 10) + "...");
        
        // Clean up old entries
        cleanupExpiredEntries();
    }
    
    /**
     * Get cached token response if available
     */
    public Map<String, Object> getCachedTokenResponse(String code) {
        String cacheKey = buildCacheKey(code);
        TokenCacheEntry entry = codeCache.get(cacheKey);
        
        if (entry != null && !entry.isExpired()) {
            log.debug("Found cached token response for code: {}", code.substring(0, 10) + "...");
            return entry.tokenResponse();
        }
        
        return null;
    }
    
    /**
     * Check if a code has been used
     */
    public boolean isCodeUsed(String code) {
        String cacheKey = buildCacheKey(code);
        return codeCache.containsKey(cacheKey);
    }
    
    /**
     * Mark a code as failed
     */
    public void markCodeAsFailed(String code) {
        String cacheKey = buildCacheKey(code);
        // Store null to indicate the code was used but failed
        TokenCacheEntry entry = new TokenCacheEntry(null, System.currentTimeMillis());
        codeCache.put(cacheKey, entry);
        log.debug("Marked code as failed: {}", code.substring(0, 10) + "...");
    }
    
    private String buildCacheKey(String code) {
        return "oauth_code:" + code;
    }
    
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        codeCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    private record TokenCacheEntry(Map<String, Object> tokenResponse, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CODE_TTL_SECONDS * 1000;
        }
    }
}