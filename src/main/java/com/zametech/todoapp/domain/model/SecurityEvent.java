package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {
    
    private UUID id;
    
    private EventType eventType;
    
    private User user;
    
    private String clientId;
    
    private String ipAddress;
    
    private String userAgent;
    
    private Boolean success;
    
    private String errorCode;
    
    private String errorDescription;
    
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    
    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        TOKEN_REFRESH,
        TOKEN_REVOKE,
        AUTHORIZATION_CODE_ISSUED,
        AUTHORIZATION_CODE_USED,
        AUTHORIZATION_CODE_EXPIRED,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        SOCIAL_ACCOUNT_LINKED,
        SOCIAL_ACCOUNT_UNLINKED,
        SUSPICIOUS_ACTIVITY
    }
}