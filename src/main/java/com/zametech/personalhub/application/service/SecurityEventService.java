package com.zametech.personalhub.application.service;

import com.zametech.personalhub.domain.model.SecurityEvent;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.SecurityEventRepository;
import com.zametech.personalhub.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {
    
    private final SecurityEventRepository securityEventRepository;
    private final UserRepository userRepository;
    
    // Track failed login attempts by IP
    private final Map<String, Integer> failedAttemptsPerIp = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailedAttemptTime = new ConcurrentHashMap<>();
    
    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    public void logSecurityEvent(SecurityEvent.EventType eventType, User user, String clientId, boolean success) {
        logSecurityEvent(eventType, user, clientId, success, null, null, null);
    }
    
    public void logSecurityEvent(SecurityEvent.EventType eventType, User user, String clientId, boolean success, 
                                String errorCode, String errorDescription, Map<String, Object> metadata) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            
            SecurityEvent event = SecurityEvent.builder()
                .eventType(eventType)
                .user(user)
                .clientId(clientId)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .success(success)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .metadata(metadata)
                .build();
            
            securityEventRepository.save(event);
            
            if (!success) {
                log.warn("Security event: {} failed for user: {}, client: {}, IP: {}, Error: {}", 
                    eventType, user != null ? user.getId() : "unknown", clientId, 
                    event.getIpAddress(), errorDescription);
            } else {
                log.info("Security event: {} succeeded for user: {}, client: {}, IP: {}", 
                    eventType, user != null ? user.getId() : "unknown", clientId, event.getIpAddress());
            }
        } catch (Exception e) {
            log.error("Failed to log security event", e);
        }
    }
    
    public boolean isAccountLocked(UUID userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long failedAttempts = securityEventRepository.countFailedLoginAttempts(userId, oneHourAgo);
        return failedAttempts >= 5;
    }
    
    /**
     * ログイン成功を記録
     */
    public void recordLoginSuccess(User user, String provider, String ipAddress, String userAgent, Map<String, Object> metadata) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.LOGIN_SUCCESS)
                .user(user)
                .clientId(provider)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .metadata(metadata)
                .build();
        
        securityEventRepository.save(event);
        log.info("Login successful for user: {} via {}", user.getEmail(), provider);
    }
    
    /**
     * ログイン失敗を記録
     */
    public void recordLoginFailure(User user, String provider, String ipAddress, String userAgent, 
                                  String errorCode, String errorDescription, Map<String, Object> metadata) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.LOGIN_FAILURE)
                .user(user)
                .clientId(provider)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .metadata(metadata)
                .build();
        
        securityEventRepository.save(event);
        log.warn("Login failed for provider: {}, IP: {}, Error: {}", provider, ipAddress, errorDescription);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Check if IP address is locked due to too many failed attempts
     */
    public boolean isIpAddressLocked(String ipAddress) {
        LocalDateTime lastAttempt = lastFailedAttemptTime.get(ipAddress);
        if (lastAttempt != null && lastAttempt.isAfter(LocalDateTime.now().minusMinutes(lockoutDurationMinutes))) {
            Integer attempts = failedAttemptsPerIp.getOrDefault(ipAddress, 0);
            return attempts >= maxFailedAttempts;
        }
        // Reset if lockout period has passed
        failedAttemptsPerIp.remove(ipAddress);
        lastFailedAttemptTime.remove(ipAddress);
        return false;
    }
    
    /**
     * Track failed login attempt by IP
     */
    @Async
    public void trackFailedLoginAttempt(String ipAddress) {
        failedAttemptsPerIp.merge(ipAddress, 1, Integer::sum);
        lastFailedAttemptTime.put(ipAddress, LocalDateTime.now());
        
        Integer attempts = failedAttemptsPerIp.get(ipAddress);
        if (attempts >= maxFailedAttempts) {
            log.warn("IP address {} has been locked after {} failed attempts", ipAddress, attempts);
        }
    }
    
    /**
     * Clear failed attempts for IP after successful login
     */
    @Async
    public void clearFailedAttempts(String ipAddress) {
        failedAttemptsPerIp.remove(ipAddress);
        lastFailedAttemptTime.remove(ipAddress);
    }
    
    /**
     * Record token refresh event
     */
    public void recordTokenRefresh(User user, String ipAddress, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.TOKEN_REFRESH)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .build();
        
        securityEventRepository.save(event);
        log.debug("Token refreshed for user: {}", user.getEmail());
    }
    
    /**
     * Record logout event
     */
    public void recordLogout(User user, String ipAddress, String userAgent) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.LOGOUT)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .build();
        
        securityEventRepository.save(event);
        log.info("User {} logged out", user.getEmail());
    }
    
    /**
     * Get recent security events for a user
     */
    public List<SecurityEvent> getRecentSecurityEvents(UUID userId, int limit) {
        return securityEventRepository.findRecentEventsByUser(userId, limit);
    }
    
    /**
     * Get suspicious activity summary
     */
    public Map<String, Object> getSuspiciousActivitySummary() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        Map<String, Object> summary = new HashMap<>();
        
        // Count failed login attempts in last 24 hours
        long failedLogins = securityEventRepository.countByEventTypeAndSuccessAndCreatedAtAfter(
                SecurityEvent.EventType.LOGIN_FAILURE, false, oneDayAgo);
        
        // Get IPs with multiple failed attempts
        List<String> suspiciousIps = failedAttemptsPerIp.entrySet().stream()
                .filter(entry -> entry.getValue() >= 3)
                .map(Map.Entry::getKey)
                .toList();
        
        summary.put("failedLoginsLast24h", failedLogins);
        summary.put("suspiciousIps", suspiciousIps);
        summary.put("lockedIpCount", suspiciousIps.size());
        
        return summary;
    }
}