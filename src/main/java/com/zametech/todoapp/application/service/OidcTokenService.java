package com.zametech.todoapp.application.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.todoapp.domain.model.AuthorizationCode;
import com.zametech.todoapp.domain.model.RefreshToken;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.RefreshTokenRepository;
import com.zametech.todoapp.presentation.dto.oidc.TokenRequest;
import com.zametech.todoapp.presentation.dto.oidc.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OidcTokenService {
    
    private final OidcAuthorizationService authorizationService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwksService jwksService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.oidc.issuer}")
    private String issuer;
    
    @Value("${app.oidc.access-token-ttl:900}")
    private int accessTokenTtl;
    
    @Value("${app.oidc.refresh-token-ttl:2592000}")
    private int refreshTokenTtl;
    
    @Value("${app.oidc.id-token-ttl:3600}")
    private int idTokenTtl;
    
    @Transactional
    public TokenResponse processTokenRequest(TokenRequest request) {
        if ("authorization_code".equals(request.grantType())) {
            return processAuthorizationCodeGrant(request);
        } else if ("refresh_token".equals(request.grantType())) {
            return processRefreshTokenGrant(request);
        } else {
            throw new IllegalArgumentException("Unsupported grant type: " + request.grantType());
        }
    }
    
    private TokenResponse processAuthorizationCodeGrant(TokenRequest request) {
        if (request.code() == null || request.redirectUri() == null || request.clientId() == null) {
            throw new IllegalArgumentException("Missing required parameters for authorization_code grant");
        }
        
        Optional<AuthorizationCode> authCodeOpt = authorizationService.validateAndConsumeAuthorizationCode(
            request.code(), request.clientId(), request.redirectUri(), request.codeVerifier()
        );
        
        if (authCodeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid authorization code");
        }
        
        AuthorizationCode authCode = authCodeOpt.get();
        User user = authCode.getUser();
        
        String accessToken = generateAccessToken(user, authCode.getClientId(), authCode.getScopes());
        String refreshTokenValue = generateRefreshToken(user, authCode.getClientId(), authCode.getScopes());
        String idToken = null;
        
        if (authCode.getScopes().contains("openid")) {
            idToken = generateIdToken(user, authCode.getClientId(), authCode.getNonce(), authCode.getAuthTime());
        }
        
        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn((long) accessTokenTtl)
            .refreshToken(refreshTokenValue)
            .scope(String.join(" ", authCode.getScopes()))
            .idToken(idToken)
            .build();
    }
    
    private TokenResponse processRefreshTokenGrant(TokenRequest request) {
        if (request.refreshToken() == null) {
            throw new IllegalArgumentException("Missing refresh_token parameter");
        }
        
        String tokenHash = passwordEncoder.encode(request.refreshToken());
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        
        if (refreshTokenOpt.isEmpty() || !refreshTokenOpt.get().isValid()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        RefreshToken refreshToken = refreshTokenOpt.get();
        User user = refreshToken.getUser();
        
        String accessToken = generateAccessToken(user, refreshToken.getClientId(), refreshToken.getScopes());
        String newRefreshToken = generateRefreshToken(user, refreshToken.getClientId(), refreshToken.getScopes());
        
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        
        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn((long) accessTokenTtl)
            .refreshToken(newRefreshToken)
            .scope(String.join(" ", refreshToken.getScopes()))
            .build();
    }
    
    private String generateAccessToken(User user, String clientId, List<String> scopes) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(accessTokenTtl);
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .audience(clientId)
                .expirationTime(Date.from(expiration))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", String.join(" ", scopes))
                .claim("client_id", clientId)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("email_verified", user.getEmailVerified())
                .build();
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(jwksService.getKeyId())
                .type(JOSEObjectType.JWT)
                .build();
            
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            RSASSASigner signer = new RSASSASigner((RSAPrivateKey) jwksService.getKeyPair().getPrivate());
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }
    
    private String generateRefreshToken(User user, String clientId, List<String> scopes) {
        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(tokenValue);
        
        RefreshToken refreshToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .user(user)
            .clientId(clientId)
            .scopes(scopes)
            .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtl))
            .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return tokenValue;
    }
    
    private String generateIdToken(User user, String clientId, String nonce, LocalDateTime authTime) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(idTokenTtl);
            
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .audience(clientId)
                .expirationTime(Date.from(expiration))
                .issueTime(Date.from(now))
                .claim("auth_time", authTime.toEpochSecond(ZoneOffset.UTC))
                .claim("email", user.getEmail())
                .claim("email_verified", user.getEmailVerified())
                .claim("name", user.getUsername())
                .claim("preferred_username", user.getUsername());
            
            if (user.getGivenName() != null) {
                claimsBuilder.claim("given_name", user.getGivenName());
            }
            
            if (user.getFamilyName() != null) {
                claimsBuilder.claim("family_name", user.getFamilyName());
            }
            
            if (user.getProfilePictureUrl() != null) {
                claimsBuilder.claim("picture", user.getProfilePictureUrl());
            }
            
            if (user.getLocale() != null) {
                claimsBuilder.claim("locale", user.getLocale());
            }
            
            if (nonce != null) {
                claimsBuilder.claim("nonce", nonce);
            }
            
            JWTClaimsSet claimsSet = claimsBuilder.build();
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(jwksService.getKeyId())
                .type(JOSEObjectType.JWT)
                .build();
            
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            RSASSASigner signer = new RSASSASigner((RSAPrivateKey) jwksService.getKeyPair().getPrivate());
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Error generating ID token", e);
            throw new RuntimeException("Failed to generate ID token", e);
        }
    }
}