package com.zametech.personalhub.domain.model;

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
public class UserSocialAccount {
    
    private UUID id;
    
    private User user;
    
    private String provider;
    
    private String providerUserId;
    
    private String email;
    
    @Builder.Default
    private Boolean emailVerified = false;
    
    private String name;
    
    private String givenName;
    
    private String familyName;
    
    private String picture;
    
    private String locale;
    
    private Map<String, Object> profileData;
    
    private String accessTokenEncrypted;
    
    private String refreshTokenEncrypted;
    
    private LocalDateTime tokenExpiresAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}