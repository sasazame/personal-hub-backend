package com.zametech.personalhub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String password;
    private String username;
    private boolean enabled;
    private Boolean emailVerified;
    private String profilePictureUrl;
    private String givenName;
    private String familyName;
    private String locale;
    private Integer weekStartDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}