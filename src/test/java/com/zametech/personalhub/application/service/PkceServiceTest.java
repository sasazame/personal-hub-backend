package com.zametech.personalhub.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PkceServiceTest {
    
    private PkceService pkceService;
    
    @BeforeEach
    void setUp() {
        pkceService = new PkceService();
    }
    
    @Test
    void generateCodeVerifier_shouldGenerateValidCodeVerifier() {
        // When
        String codeVerifier = pkceService.generateCodeVerifier();
        
        // Then
        assertThat(codeVerifier).isNotNull();
        assertThat(codeVerifier.length()).isBetween(43, 128);
        assertThat(pkceService.isValidCodeVerifier(codeVerifier)).isTrue();
    }
    
    @RepeatedTest(10)
    void generateCodeVerifier_shouldGenerateUniqueValues() {
        // Given
        Set<String> codeVerifiers = new HashSet<>();
        
        // When
        for (int i = 0; i < 100; i++) {
            codeVerifiers.add(pkceService.generateCodeVerifier());
        }
        
        // Then
        assertThat(codeVerifiers).hasSize(100);
    }
    
    @Test
    void generateCodeVerifier_shouldOnlyUseAllowedCharacters() {
        // Given
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        
        // When
        String codeVerifier = pkceService.generateCodeVerifier();
        
        // Then
        for (char c : codeVerifier.toCharArray()) {
            assertThat(allowedChars).contains(String.valueOf(c));
        }
    }
    
    @Test
    void generateCodeChallenge_withPlainMethod_shouldReturnCodeVerifier() {
        // Given
        String codeVerifier = "test-code-verifier";
        
        // When
        String codeChallenge = pkceService.generateCodeChallenge(codeVerifier, "plain");
        
        // Then
        assertThat(codeChallenge).isEqualTo(codeVerifier);
    }
    
    @Test
    void generateCodeChallenge_withS256Method_shouldReturnSha256Hash() throws Exception {
        // Given
        String codeVerifier = "test-code-verifier";
        
        // When
        String codeChallenge = pkceService.generateCodeChallenge(codeVerifier, "S256");
        
        // Then
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        assertThat(codeChallenge).isEqualTo(expectedChallenge);
    }
    
    @Test
    void generateCodeChallenge_withUnsupportedMethod_shouldThrowException() {
        // Given
        String codeVerifier = "test-code-verifier";
        
        // When & Then
        assertThatThrownBy(() -> pkceService.generateCodeChallenge(codeVerifier, "unsupported"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported code challenge method: unsupported");
    }
    
    @Test
    void verifyCodeChallenge_withValidPlainChallenge_shouldReturnTrue() {
        // Given
        String codeVerifier = "test-code-verifier";
        String codeChallenge = codeVerifier;
        
        // When
        boolean result = pkceService.verifyCodeChallenge(codeVerifier, codeChallenge, "plain");
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void verifyCodeChallenge_withValidS256Challenge_shouldReturnTrue() {
        // Given
        String codeVerifier = "test-code-verifier";
        String codeChallenge = pkceService.generateCodeChallenge(codeVerifier, "S256");
        
        // When
        boolean result = pkceService.verifyCodeChallenge(codeVerifier, codeChallenge, "S256");
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void verifyCodeChallenge_withInvalidChallenge_shouldReturnFalse() {
        // Given
        String codeVerifier = "test-code-verifier";
        String invalidChallenge = "invalid-challenge";
        
        // When
        boolean result = pkceService.verifyCodeChallenge(codeVerifier, invalidChallenge, "S256");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void verifyCodeChallenge_withNullCodeVerifier_shouldReturnFalse() {
        // Given
        String codeChallenge = "some-challenge";
        
        // When
        boolean result = pkceService.verifyCodeChallenge(null, codeChallenge, "S256");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void verifyCodeChallenge_withNullCodeChallenge_shouldReturnFalse() {
        // Given
        String codeVerifier = "test-code-verifier";
        
        // When
        boolean result = pkceService.verifyCodeChallenge(codeVerifier, null, "S256");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void verifyCodeChallenge_withUnsupportedMethod_shouldReturnFalse() {
        // Given
        String codeVerifier = "test-code-verifier";
        String codeChallenge = "some-challenge";
        
        // When
        boolean result = pkceService.verifyCodeChallenge(codeVerifier, codeChallenge, "unsupported");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void isValidCodeVerifier_withValidCodeVerifier_shouldReturnTrue() {
        // Given
        String validCodeVerifier = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        
        // When
        boolean result = pkceService.isValidCodeVerifier(validCodeVerifier);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void isValidCodeVerifier_withMinimumLengthCodeVerifier_shouldReturnTrue() {
        // Given
        String codeVerifier = "a".repeat(43);
        
        // When
        boolean result = pkceService.isValidCodeVerifier(codeVerifier);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void isValidCodeVerifier_withMaximumLengthCodeVerifier_shouldReturnTrue() {
        // Given
        String codeVerifier = "a".repeat(128);
        
        // When
        boolean result = pkceService.isValidCodeVerifier(codeVerifier);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void isValidCodeVerifier_withTooShortCodeVerifier_shouldReturnFalse() {
        // Given
        String codeVerifier = "a".repeat(42);
        
        // When
        boolean result = pkceService.isValidCodeVerifier(codeVerifier);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void isValidCodeVerifier_withTooLongCodeVerifier_shouldReturnFalse() {
        // Given
        String codeVerifier = "a".repeat(129);
        
        // When
        boolean result = pkceService.isValidCodeVerifier(codeVerifier);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void isValidCodeVerifier_withInvalidCharacters_shouldReturnFalse() {
        // Given
        String codeVerifier = "a".repeat(42) + "!"; // 43 chars with invalid character
        
        // When
        boolean result = pkceService.isValidCodeVerifier(codeVerifier);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void isValidCodeVerifier_withNullCodeVerifier_shouldReturnFalse() {
        // When
        boolean result = pkceService.isValidCodeVerifier(null);
        
        // Then
        assertThat(result).isFalse();
    }
}