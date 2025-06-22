package com.zametech.todoapp.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "authorization_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCode {
    
    @Id
    private String code;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "redirect_uri", nullable = false)
    private String redirectUri;
    
    @Column(name = "scopes", nullable = false)
    @Convert(converter = OAuthApplication.StringListConverter.class)
    private List<String> scopes;
    
    @Column(name = "code_challenge")
    private String codeChallenge;
    
    @Column(name = "code_challenge_method")
    private String codeChallengeMethod;
    
    @Column(name = "nonce")
    private String nonce;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "auth_time", nullable = false)
    private LocalDateTime authTime;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "used")
    private Boolean used = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired();
    }
}