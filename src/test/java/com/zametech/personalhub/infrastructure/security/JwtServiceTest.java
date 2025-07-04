package com.zametech.personalhub.infrastructure.security;

import com.zametech.personalhub.application.service.JwksService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    
    @Mock
    private JwksService jwksService;
    
    private JwtConfiguration jwtConfiguration;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock JwksService with RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        when(jwksService.getKeyPair()).thenReturn(keyPair);
        when(jwksService.getKeyId()).thenReturn("test-key");
        
        // Create JwtConfiguration
        jwtConfiguration = new JwtConfiguration();
        jwtConfiguration.setSecretKey("test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        jwtConfiguration.setExpiration(3600000);
        jwtConfiguration.setKeyId("test-key");
        
        jwtService = new JwtService(jwksService, jwtConfiguration);
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
    }

    @Test
    void shouldGenerateValidJwtToken() {
        String token = jwtService.generateToken(userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void shouldGenerateTokenWithExtraClaims() {
        Map<String, Object> extraClaims = Map.of("role", "USER", "customField", "value");
        
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken(userDetails);
        
        String username = jwtService.extractUsername(token);
        
        assertEquals("testuser", username);
    }

    @Test
    void shouldValidateValidToken() {
        String token = jwtService.generateToken(userDetails);
        
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        assertTrue(isValid);
    }

    @Test
    void shouldRejectTokenWithDifferentUsername() {
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
        
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        
        assertFalse(isValid);
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        // Create configuration with very short expiration
        JwtConfiguration shortLivedConfig = new JwtConfiguration();
        shortLivedConfig.setSecretKey("test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        shortLivedConfig.setExpiration(1); // 1 millisecond
        shortLivedConfig.setKeyId("test-key");
        
        JwtService shortLivedJwtService = new JwtService(jwksService, shortLivedConfig);
        String token = shortLivedJwtService.generateToken(userDetails);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThrows(ExpiredJwtException.class, () -> {
            shortLivedJwtService.extractUsername(token);
        });
    }

    @Test
    void shouldRejectMalformedToken() {
        String malformedToken = "invalid.token.here";
        
        assertThrows(MalformedJwtException.class, () -> {
            jwtService.extractUsername(malformedToken);
        });
    }

    @Test
    void shouldRejectTokenWithWrongSignature() throws Exception {
        // Create JwtConfiguration with different secret key
        JwtConfiguration differentConfig = new JwtConfiguration();
        differentConfig.setSecretKey("different-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        differentConfig.setExpiration(3600000);
        differentConfig.setKeyId("test-key");
        
        JwtService differentKeyService = new JwtService(jwksService, differentConfig);
        String token = differentKeyService.generateToken(userDetails);
        
        assertThrows(SignatureException.class, () -> {
            jwtService.extractUsername(token);
        });
    }

    @Test
    void shouldExtractExpirationDateFromToken() {
        String token = jwtService.generateToken(userDetails);
        
        assertDoesNotThrow(() -> {
            jwtService.extractExpiration(token);
        });
    }

    @Test
    void shouldCheckIfTokenIsExpired() {
        String token = jwtService.generateToken(userDetails);
        
        boolean isExpired = jwtService.isTokenExpired(token);
        
        assertFalse(isExpired);
    }
}