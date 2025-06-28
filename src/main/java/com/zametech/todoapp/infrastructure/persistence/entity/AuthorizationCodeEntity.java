package com.zametech.todoapp.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "authorization_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCodeEntity {
    
    @Id
    @Column(nullable = false)
    private String code;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "redirect_uri", nullable = false, columnDefinition = "TEXT")
    private String redirectUri;
    
    @Column(nullable = false, length = 1000)
    @Convert(converter = StringListConverter.class)
    private List<String> scopes;
    
    @Column(name = "code_challenge")
    private String codeChallenge;
    
    @Column(name = "code_challenge_method", length = 10)
    private String codeChallengeMethod;
    
    @Column
    private String nonce;
    
    @Column
    private String state;
    
    @Column(name = "auth_time", nullable = false)
    private LocalDateTime authTime;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean used = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Converter
    public static class StringListConverter implements AttributeConverter<List<String>, String> {
        private static final String DELIMITER = ",";
        
        @Override
        public String convertToDatabaseColumn(List<String> list) {
            if (list == null || list.isEmpty()) {
                return "";
            }
            return String.join(DELIMITER, list);
        }
        
        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return List.of();
            }
            return List.of(dbData.split(DELIMITER));
        }
    }
}