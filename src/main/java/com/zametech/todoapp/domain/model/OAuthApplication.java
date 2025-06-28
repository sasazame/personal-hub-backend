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
public class OAuthApplication {
    
    private UUID id;
    
    private String clientId;
    
    private String clientSecretHash;
    
    private List<String> redirectUris;
    
    private List<String> scopes;
    
    @Builder.Default
    private String applicationType = "web";
    
    private List<String> grantTypes;
    
    private List<String> responseTypes;
    
    @Builder.Default
    private String tokenEndpointAuthMethod = "client_secret_basic";
    
    private String applicationName;
    
    private String applicationUri;
    
    private List<String> contacts;
    
    private String logoUri;
    
    private String tosUri;
    
    private String policyUri;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public static class StringListConverter {
        private static final String DELIMITER = ",";
        
        public String convertToDatabaseColumn(List<String> list) {
            if (list == null || list.isEmpty()) {
                return "";
            }
            return String.join(DELIMITER, list);
        }
        
        public List<String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return List.of();
            }
            return List.of(dbData.split(DELIMITER));
        }
    }
}