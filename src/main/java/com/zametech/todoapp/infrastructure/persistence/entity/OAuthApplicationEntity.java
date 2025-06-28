package com.zametech.todoapp.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "oauth_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthApplicationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;
    
    @Column(name = "client_secret_hash")
    private String clientSecretHash;
    
    @Column(name = "redirect_uris", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> redirectUris;
    
    @Column(nullable = false, length = 1000)
    @Convert(converter = StringListConverter.class)
    private List<String> scopes;
    
    @Column(name = "application_type", length = 50)
    private String applicationType = "web";
    
    @Column(name = "grant_types", nullable = false, length = 500)
    @Convert(converter = StringListConverter.class)
    private List<String> grantTypes;
    
    @Column(name = "response_types", nullable = false, length = 500)
    @Convert(converter = StringListConverter.class)
    private List<String> responseTypes;
    
    @Column(name = "token_endpoint_auth_method", length = 50)
    private String tokenEndpointAuthMethod = "client_secret_basic";
    
    @Column(name = "application_name", nullable = false)
    private String applicationName;
    
    @Column(name = "application_uri", length = 500)
    private String applicationUri;
    
    @Column(length = 1000)
    @Convert(converter = StringListConverter.class)
    private List<String> contacts;
    
    @Column(name = "logo_uri", length = 500)
    private String logoUri;
    
    @Column(name = "tos_uri", length = 500)
    private String tosUri;
    
    @Column(name = "policy_uri", length = 500)
    private String policyUri;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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