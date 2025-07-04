package com.zametech.personalhub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @Column(nullable = false, length = 1000)
    @Convert(converter = StringListConverter.class)
    private List<String> scopes;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean revoked = false;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
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