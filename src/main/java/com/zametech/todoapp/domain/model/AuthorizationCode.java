package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCode {
    
    private String code;
    
    private String clientId;
    
    private User user;
    
    private String redirectUri;
    
    private List<String> scopes;
    
    private String codeChallenge;
    
    private String codeChallengeMethod;
    
    private String nonce;
    
    private String state;
    
    private LocalDateTime authTime;
    
    private LocalDateTime expiresAt;
    
    @Builder.Default
    private Boolean used = false;
    
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}