package com.zametech.todoapp.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;
    
    @Column(name = "client_secret_hash")
    private String clientSecretHash;
    
    @Column(name = "redirect_uris", nullable = false)
    @Convert(converter = StringListConverter.class)
    private List<String> redirectUris;
    
    @Column(name = "scopes", nullable = false)
    @Convert(converter = StringListConverter.class)
    private List<String> scopes;
    
    @Column(name = "application_type")
    private String applicationType = "web";
    
    @Column(name = "grant_types", nullable = false)
    @Convert(converter = StringListConverter.class)
    private List<String> grantTypes;
    
    @Column(name = "response_types", nullable = false)
    @Convert(converter = StringListConverter.class)
    private List<String> responseTypes;
    
    @Column(name = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod = "client_secret_basic";
    
    @Column(name = "application_name", nullable = false)
    private String applicationName;
    
    @Column(name = "application_uri")
    private String applicationUri;
    
    @Column(name = "contacts")
    @Convert(converter = StringListConverter.class)
    private List<String> contacts;
    
    @Column(name = "logo_uri")
    private String logoUri;
    
    @Column(name = "tos_uri")
    private String tosUri;
    
    @Column(name = "policy_uri")
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