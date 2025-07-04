package com.zametech.personalhub.domain.model;

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
public class RefreshToken {
    
    private UUID id;
    
    private String tokenHash;
    
    private User user;
    
    private String clientId;
    
    private List<String> scopes;
    
    private LocalDateTime expiresAt;
    
    @Builder.Default
    private Boolean revoked = false;
    
    private LocalDateTime revokedAt;
    
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !revoked && !isExpired();
    }
    
    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }
}