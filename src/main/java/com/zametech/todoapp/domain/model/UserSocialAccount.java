package com.zametech.todoapp.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_social_accounts", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocialAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "provider", nullable = false)
    private String provider;
    
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "given_name")
    private String givenName;
    
    @Column(name = "family_name")
    private String familyName;
    
    @Column(name = "picture")
    private String picture;
    
    @Column(name = "locale")
    private String locale;
    
    @Column(name = "profile_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> profileData;
    
    @Column(name = "access_token_encrypted")
    private String accessTokenEncrypted;
    
    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}