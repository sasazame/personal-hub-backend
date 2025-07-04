package com.zametech.personalhub.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class PkceService {
    
    private static final String CODE_VERIFIER_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final int CODE_VERIFIER_MIN_LENGTH = 43;
    private static final int CODE_VERIFIER_MAX_LENGTH = 128;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    public String generateCodeVerifier() {
        int length = CODE_VERIFIER_MIN_LENGTH + secureRandom.nextInt(CODE_VERIFIER_MAX_LENGTH - CODE_VERIFIER_MIN_LENGTH + 1);
        StringBuilder codeVerifier = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            codeVerifier.append(CODE_VERIFIER_CHARSET.charAt(secureRandom.nextInt(CODE_VERIFIER_CHARSET.length())));
        }
        
        return codeVerifier.toString();
    }
    
    public String generateCodeChallenge(String codeVerifier, String method) {
        if ("plain".equals(method)) {
            return codeVerifier;
        } else if ("S256".equals(method)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                log.error("SHA-256 algorithm not available", e);
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported code challenge method: " + method);
        }
    }
    
    public boolean verifyCodeChallenge(String codeVerifier, String codeChallenge, String method) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }
        
        try {
            String expectedChallenge = generateCodeChallenge(codeVerifier, method);
            return MessageDigest.isEqual(
                codeChallenge.getBytes(StandardCharsets.UTF_8),
                expectedChallenge.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error verifying code challenge", e);
            return false;
        }
    }
    
    public boolean isValidCodeVerifier(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }
        
        int length = codeVerifier.length();
        if (length < CODE_VERIFIER_MIN_LENGTH || length > CODE_VERIFIER_MAX_LENGTH) {
            return false;
        }
        
        return codeVerifier.chars().allMatch(c -> CODE_VERIFIER_CHARSET.indexOf(c) >= 0);
    }
}