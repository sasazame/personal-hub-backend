package com.zametech.todoapp.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm", Duration.ofHours(1));
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
    void shouldRejectExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(
            "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm", 
            Duration.ofMillis(1)
        );
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
    void shouldRejectTokenWithWrongSignature() {
        JwtService differentKeyService = new JwtService(
            "different-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm", 
            Duration.ofHours(1)
        );
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