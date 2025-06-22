package com.zametech.todoapp.application.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.zametech.todoapp.presentation.dto.oidc.JwksResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class JwksService {
    
    @Value("${app.jwt.key-id:default-key}")
    private String keyId;
    
    private final KeyPair keyPair;
    
    public JwksService() {
        this.keyPair = generateKeyPair();
    }
    
    public JwksResponse getJwks() {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            
            JwksResponse.JwkKey jwkKey = JwksResponse.JwkKey.builder()
                .kty("RSA")
                .use("sig")
                .alg("RS256")
                .kid(keyId)
                .n(Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray()))
                .e(Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray()))
                .build();
            
            return JwksResponse.builder()
                .keys(List.of(jwkKey))
                .build();
                
        } catch (Exception e) {
            log.error("Error generating JWKS", e);
            throw new RuntimeException("Failed to generate JWKS", e);
        }
    }
    
    public KeyPair getKeyPair() {
        return keyPair;
    }
    
    public String getKeyId() {
        return keyId;
    }
    
    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
            return keyGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating key pair", e);
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }
}