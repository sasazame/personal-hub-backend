package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.SecurityEvent;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.SecurityEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {
    
    private final SecurityEventRepository securityEventRepository;
    
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
}