package com.zametech.personalhub.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.presentation.dto.oidc.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OidcUserInfoService {
    
    private final UserRepository userRepository;
    
    public UserInfoResponse getUserInfo(String accessToken, List<String> scopes) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            String userIdString = claims.getSubject();
            UUID userId = UUID.fromString(userIdString);
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }
            
            User user = userOpt.get();
            
            UserInfoResponse.UserInfoResponseBuilder builder = UserInfoResponse.builder()
                .sub(user.getId().toString());
            
            if (scopes.contains("profile")) {
                builder
                    .name(user.getUsername())
                    .preferredUsername(user.getUsername());
                
                if (user.getGivenName() != null) {
                    builder.givenName(user.getGivenName());
                }
                
                if (user.getFamilyName() != null) {
                    builder.familyName(user.getFamilyName());
                }
                
                if (user.getProfilePictureUrl() != null) {
                    builder.picture(user.getProfilePictureUrl());
                }
                
                if (user.getLocale() != null) {
                    builder.locale(user.getLocale());
                }
                
                if (user.getUpdatedAt() != null) {
                    builder.updatedAt(user.getUpdatedAt().toEpochSecond(ZoneOffset.UTC));
                }
            }
            
            if (scopes.contains("email")) {
                builder
                    .email(user.getEmail())
                    .emailVerified(user.getEmailVerified());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new RuntimeException("Failed to get user info", e);
        }
    }
}