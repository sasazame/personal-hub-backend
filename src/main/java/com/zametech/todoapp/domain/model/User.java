package com.zametech.todoapp.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String password;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    private boolean enabled = true;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
    
    @Column(name = "given_name")
    private String givenName;
    
    @Column(name = "family_name")
    private String familyName;
    
    private String locale;
    
    @Column(name = "week_start_day")
    private Integer weekStartDay = 1;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
}