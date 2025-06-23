package com.zametech.todoapp.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for managing OAuth state parameters to prevent CSRF attacks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateParameterService {
    
    private static final int STATE_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate a new state parameter
     */
    public String generateState() {
        byte[] bytes = new byte[STATE_LENGTH];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        log.debug("Generated new state parameter: {}", state);
        return state;
    }
    
    /**
     * Store state parameter with associated data
     * State parameters expire after 10 minutes
     */
    @Cacheable(value = "oauthStates", key = "#state")
    public StateData storeState(String state, String provider, String ipAddress) {
        StateData stateData = new StateData(state, provider, ipAddress, System.currentTimeMillis());
        log.debug("Stored state parameter for provider: {}", provider);
        return stateData;
    }
    
    /**
     * Validate and consume state parameter
     */
    @CacheEvict(value = "oauthStates", key = "#state")
    public boolean validateState(String state, String provider, String ipAddress) {
        try {
            StateData stateData = getStateData(state);
            if (stateData == null) {
                log.warn("State parameter not found: {}", state);
                return false;
            }
            
            // Check if state expired (10 minutes)
            long elapsedTime = System.currentTimeMillis() - stateData.timestamp();
            if (elapsedTime > 600000) { // 10 minutes
                log.warn("State parameter expired: {}", state);
                return false;
            }
            
            // Validate provider
            if (!provider.equals(stateData.provider())) {
                log.warn("Provider mismatch for state: expected {}, got {}", stateData.provider(), provider);
                return false;
            }
            
            // Validate IP address (optional - can be disabled for mobile apps)
            if (!ipAddress.equals(stateData.ipAddress())) {
                log.warn("IP address mismatch for state: expected {}, got {}", stateData.ipAddress(), ipAddress);
                // For now, just log warning but don't fail validation
            }
            
            log.debug("State parameter validated successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Error validating state parameter", e);
            return false;
        }
    }
    
    @Cacheable(value = "oauthStates", key = "#state")
    public StateData getStateData(String state) {
        return null; // Will be populated from cache
    }
    
    /**
     * Generate and store state in one operation
     */
    public String generateAndStoreState(String provider, String ipAddress) {
        String state = generateState();
        storeState(state, provider, ipAddress);
        return state;
    }
    
    public record StateData(
        String state,
        String provider,
        String ipAddress,
        long timestamp
    ) {}
}