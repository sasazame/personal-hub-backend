package com.zametech.personalhub.application.service;

import com.zametech.personalhub.domain.model.AuthorizationCode;
import com.zametech.personalhub.domain.model.OAuthApplication;
import com.zametech.personalhub.domain.model.SecurityEvent;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.AuthorizationCodeRepository;
import com.zametech.personalhub.domain.repository.OAuthApplicationRepository;
import com.zametech.personalhub.presentation.dto.oidc.AuthorizationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OidcAuthorizationService {
    
    private final OAuthApplicationRepository oAuthApplicationRepository;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final PkceService pkceService;
    private final SecurityEventService securityEventService;
    
    @Value("${app.oidc.authorization-code-ttl:600}")
    private int authorizationCodeTtl;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    public String generateAuthorizationCode(AuthorizationRequest request, User user) {
        validateAuthorizationRequest(request);
        
        OAuthApplication application = oAuthApplicationRepository.findByClientId(request.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid client_id"));
        
        validateRedirectUri(request.redirectUri(), application);
        validateScopes(request.scope(), application);
        
        if (request.codeChallenge() != null) {
            validatePkce(request.codeChallenge(), request.codeChallengeMethod());
        }
        
        String code = generateSecureCode();
        
        AuthorizationCode authorizationCode = AuthorizationCode.builder()
            .code(code)
            .clientId(request.clientId())
            .user(user)
            .redirectUri(request.redirectUri())
            .scopes(parseScopes(request.scope()))
            .codeChallenge(request.codeChallenge())
            .codeChallengeMethod(request.codeChallengeMethod())
            .nonce(request.nonce())
            .state(request.state())
            .authTime(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusSeconds(authorizationCodeTtl))
            .build();
        
        authorizationCodeRepository.save(authorizationCode);
        
        securityEventService.logSecurityEvent(
            SecurityEvent.EventType.AUTHORIZATION_CODE_ISSUED,
            user,
            request.clientId(),
            true
        );
        
        return code;
    }
    
    public Optional<AuthorizationCode> validateAndConsumeAuthorizationCode(String code, String clientId, String redirectUri, String codeVerifier) {
        Optional<AuthorizationCode> authCodeOpt = authorizationCodeRepository.findByCode(code);
        
        if (authCodeOpt.isEmpty()) {
            log.warn("Authorization code not found: {}", code);
            return Optional.empty();
        }
        
        AuthorizationCode authCode = authCodeOpt.get();
        
        if (!authCode.isValid()) {
            log.warn("Authorization code is invalid or expired: {}", code);
            securityEventService.logSecurityEvent(
                SecurityEvent.EventType.AUTHORIZATION_CODE_EXPIRED,
                authCode.getUser(),
                clientId,
                false,
                "invalid_grant",
                "Authorization code is invalid or expired",
                null
            );
            return Optional.empty();
        }
        
        if (!authCode.getClientId().equals(clientId)) {
            log.warn("Client ID mismatch for authorization code: {}", code);
            return Optional.empty();
        }
        
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            log.warn("Redirect URI mismatch for authorization code: {}", code);
            return Optional.empty();
        }
        
        if (authCode.getCodeChallenge() != null) {
            if (codeVerifier == null) {
                log.warn("Code verifier required but not provided for authorization code: {}", code);
                return Optional.empty();
            }
            
            if (!pkceService.verifyCodeChallenge(codeVerifier, authCode.getCodeChallenge(), authCode.getCodeChallengeMethod())) {
                log.warn("PKCE verification failed for authorization code: {}", code);
                return Optional.empty();
            }
        }
        
        authCode.setUsed(true);
        authorizationCodeRepository.save(authCode);
        
        securityEventService.logSecurityEvent(
            SecurityEvent.EventType.AUTHORIZATION_CODE_USED,
            authCode.getUser(),
            clientId,
            true
        );
        
        return Optional.of(authCode);
    }
    
    private void validateAuthorizationRequest(AuthorizationRequest request) {
        if (request.clientId() == null || request.clientId().trim().isEmpty()) {
            throw new IllegalArgumentException("client_id is required");
        }
        
        if (request.redirectUri() == null || request.redirectUri().trim().isEmpty()) {
            throw new IllegalArgumentException("redirect_uri is required");
        }
        
        if (!"code".equals(request.responseType())) {
            throw new IllegalArgumentException("Only 'code' response type is supported");
        }
    }
    
    private void validateRedirectUri(String redirectUri, OAuthApplication application) {
        if (!application.getRedirectUris().contains(redirectUri)) {
            throw new IllegalArgumentException("Invalid redirect_uri");
        }
    }
    
    private void validateScopes(String scopeString, OAuthApplication application) {
        if (scopeString == null) {
            return;
        }
        
        List<String> requestedScopes = parseScopes(scopeString);
        for (String scope : requestedScopes) {
            if (!application.getScopes().contains(scope)) {
                throw new IllegalArgumentException("Invalid scope: " + scope);
            }
        }
    }
    
    private void validatePkce(String codeChallenge, String codeChallengeMethod) {
        if (codeChallenge == null || codeChallenge.trim().isEmpty()) {
            throw new IllegalArgumentException("code_challenge is required when using PKCE");
        }
        
        if (codeChallengeMethod == null) {
            codeChallengeMethod = "plain";
        }
        
        if (!"plain".equals(codeChallengeMethod) && !"S256".equals(codeChallengeMethod)) {
            throw new IllegalArgumentException("Unsupported code_challenge_method: " + codeChallengeMethod);
        }
    }
    
    private List<String> parseScopes(String scopeString) {
        if (scopeString == null || scopeString.trim().isEmpty()) {
            return List.of("openid");
        }
        return Arrays.asList(scopeString.trim().split("\\s+"));
    }
    
    private String generateSecureCode() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}