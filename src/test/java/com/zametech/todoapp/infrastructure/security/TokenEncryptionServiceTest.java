package com.zametech.todoapp.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    private TokenEncryptionService tokenEncryptionService;
    private TokenEncryptionService tokenEncryptionServiceWithGeneratedKey;

    @BeforeEach
    void setUp() {
        // Create service with a predefined key
        String base64Key = Base64.getEncoder().encodeToString("testkeytestkeytestkeytestkey1234".getBytes());
        tokenEncryptionService = new TokenEncryptionService(base64Key);
        
        // Create service with generated key (empty key)
        tokenEncryptionServiceWithGeneratedKey = new TokenEncryptionService("");
    }

    @Test
    void encryptToken_WithValidToken_ReturnsEncryptedString() {
        // Given
        String token = "test-access-token-12345";

        // When
        String encryptedToken = tokenEncryptionService.encryptToken(token);

        // Then
        assertThat(encryptedToken).isNotNull();
        assertThat(encryptedToken).isNotEqualTo(token);
        assertThat(encryptedToken).matches("^[A-Za-z0-9+/]+=*$"); // Base64 pattern
    }

    @Test
    void encryptToken_WithNullToken_ReturnsNull() {
        // When
        String encryptedToken = tokenEncryptionService.encryptToken(null);

        // Then
        assertThat(encryptedToken).isNull();
    }

    @Test
    void encryptToken_WithEmptyToken_ReturnsNull() {
        // When
        String encryptedToken = tokenEncryptionService.encryptToken("");

        // Then
        assertThat(encryptedToken).isNull();
    }

    @Test
    void decryptToken_WithValidEncryptedToken_ReturnsOriginalToken() {
        // Given
        String originalToken = "test-access-token-12345";
        String encryptedToken = tokenEncryptionService.encryptToken(originalToken);

        // When
        String decryptedToken = tokenEncryptionService.decryptToken(encryptedToken);

        // Then
        assertThat(decryptedToken).isEqualTo(originalToken);
    }

    @Test
    void decryptToken_WithNullToken_ReturnsNull() {
        // When
        String decryptedToken = tokenEncryptionService.decryptToken(null);

        // Then
        assertThat(decryptedToken).isNull();
    }

    @Test
    void decryptToken_WithEmptyToken_ReturnsNull() {
        // When
        String decryptedToken = tokenEncryptionService.decryptToken("");

        // Then
        assertThat(decryptedToken).isNull();
    }

    @Test
    void decryptToken_WithInvalidBase64_ReturnsNull() {
        // Given
        String invalidEncryptedToken = "not-a-valid-base64!@#$";

        // When
        String decryptedToken = tokenEncryptionService.decryptToken(invalidEncryptedToken);

        // Then
        assertThat(decryptedToken).isNull();
    }

    @Test
    void decryptToken_WithTamperedToken_ReturnsNull() {
        // Given
        String originalToken = "test-access-token-12345";
        String encryptedToken = tokenEncryptionService.encryptToken(originalToken);
        
        // Tamper with the encrypted token
        byte[] tamperedBytes = Base64.getDecoder().decode(encryptedToken);
        tamperedBytes[tamperedBytes.length - 1] ^= 0xFF; // Flip last byte
        String tamperedToken = Base64.getEncoder().encodeToString(tamperedBytes);

        // When
        String decryptedToken = tokenEncryptionService.decryptToken(tamperedToken);

        // Then
        assertThat(decryptedToken).isNull();
    }

    @Test
    void encryptDecrypt_WithDifferentKeys_FailsDecryption() {
        // Given
        String token = "test-token";
        String encryptedToken = tokenEncryptionService.encryptToken(token);

        // When - try to decrypt with different key
        String decryptedToken = tokenEncryptionServiceWithGeneratedKey.decryptToken(encryptedToken);

        // Then
        assertThat(decryptedToken).isNull();
    }

    @Test
    void constructor_WithGeneratedKey_Works() {
        // Given
        String token = "test-token";

        // When
        String encryptedToken = tokenEncryptionServiceWithGeneratedKey.encryptToken(token);
        String decryptedToken = tokenEncryptionServiceWithGeneratedKey.decryptToken(encryptedToken);

        // Then
        assertThat(decryptedToken).isEqualTo(token);
    }

    @Test
    void encryptToken_DifferentTokensProduceDifferentCiphertexts() {
        // Given
        String token1 = "token-1";
        String token2 = "token-2";

        // When
        String encrypted1 = tokenEncryptionService.encryptToken(token1);
        String encrypted2 = tokenEncryptionService.encryptToken(token2);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void encryptToken_SameTokenProducesDifferentCiphertexts() {
        // Given
        String token = "same-token";

        // When - encrypt same token twice
        String encrypted1 = tokenEncryptionService.encryptToken(token);
        String encrypted2 = tokenEncryptionService.encryptToken(token);

        // Then - should be different due to random IV
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        
        // But both should decrypt to same value
        assertThat(tokenEncryptionService.decryptToken(encrypted1)).isEqualTo(token);
        assertThat(tokenEncryptionService.decryptToken(encrypted2)).isEqualTo(token);
    }
}