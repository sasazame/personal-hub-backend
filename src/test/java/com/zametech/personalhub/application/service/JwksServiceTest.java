package com.zametech.personalhub.application.service;

import com.zametech.personalhub.presentation.dto.oidc.JwksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwksServiceTest {
    
    private JwksService jwksService;
    private String keyId = "test-key-id";
    
    @BeforeEach
    void setUp() {
        jwksService = new JwksService();
        ReflectionTestUtils.setField(jwksService, "keyId", keyId);
    }
    
    @Test
    void constructor_shouldGenerateKeyPair() {
        // When
        KeyPair keyPair = jwksService.getKeyPair();
        
        // Then
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }
    
    @Test
    void getJwks_shouldReturnValidJwksResponse() {
        // When
        JwksResponse response = jwksService.getJwks();
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.keys()).isNotNull();
        assertThat(response.keys()).hasSize(1);
        
        JwksResponse.JwkKey jwkKey = response.keys().get(0);
        assertThat(jwkKey.kty()).isEqualTo("RSA");
        assertThat(jwkKey.use()).isEqualTo("sig");
        assertThat(jwkKey.alg()).isEqualTo("RS256");
        assertThat(jwkKey.kid()).isEqualTo(keyId);
        assertThat(jwkKey.n()).isNotNull();
        assertThat(jwkKey.e()).isNotNull();
    }
    
    @Test
    void getJwks_shouldEncodePublicKeyCorrectly() {
        // Given
        KeyPair keyPair = jwksService.getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        
        // When
        JwksResponse response = jwksService.getJwks();
        JwksResponse.JwkKey jwkKey = response.keys().get(0);
        
        // Then
        String expectedModulus = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(publicKey.getModulus().toByteArray());
        String expectedExponent = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(publicKey.getPublicExponent().toByteArray());
        
        assertThat(jwkKey.n()).isEqualTo(expectedModulus);
        assertThat(jwkKey.e()).isEqualTo(expectedExponent);
    }
    
    @Test
    void getKeyId_shouldReturnConfiguredKeyId() {
        // When
        String actualKeyId = jwksService.getKeyId();
        
        // Then
        assertThat(actualKeyId).isEqualTo(keyId);
    }
    
    @Test
    void getKeyPair_shouldReturnSameKeyPairInstance() {
        // When
        KeyPair keyPair1 = jwksService.getKeyPair();
        KeyPair keyPair2 = jwksService.getKeyPair();
        
        // Then
        assertThat(keyPair1).isSameAs(keyPair2);
        assertThat(keyPair1.getPublic()).isSameAs(keyPair2.getPublic());
        assertThat(keyPair1.getPrivate()).isSameAs(keyPair2.getPrivate());
    }
    
    @Test
    void keyPair_shouldBeRSA2048() {
        // Given
        KeyPair keyPair = jwksService.getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        
        // When
        int keySize = publicKey.getModulus().bitLength();
        
        // Then
        assertThat(keySize).isGreaterThanOrEqualTo(2047); // May be 2047 or 2048 due to leading zeros
        assertThat(keySize).isLessThanOrEqualTo(2048);
    }
    
    @Test
    void multipleInstances_shouldHaveDifferentKeyPairs() {
        // Given
        JwksService service1 = new JwksService();
        JwksService service2 = new JwksService();
        
        // When
        KeyPair keyPair1 = service1.getKeyPair();
        KeyPair keyPair2 = service2.getKeyPair();
        
        // Then
        assertThat(keyPair1).isNotSameAs(keyPair2);
        assertThat(keyPair1.getPublic()).isNotEqualTo(keyPair2.getPublic());
        assertThat(keyPair1.getPrivate()).isNotEqualTo(keyPair2.getPrivate());
    }
    
    @Test
    void jwksResponse_shouldBeValidForJwtLibraries() {
        // When
        JwksResponse response = jwksService.getJwks();
        JwksResponse.JwkKey jwkKey = response.keys().get(0);
        
        // Then - Verify all required fields for JWT libraries
        assertThat(jwkKey.kty()).isNotNull();
        assertThat(jwkKey.use()).isNotNull();
        assertThat(jwkKey.alg()).isNotNull();
        assertThat(jwkKey.kid()).isNotNull();
        assertThat(jwkKey.n()).isNotNull();
        assertThat(jwkKey.e()).isNotNull();
        
        // Verify base64url encoding (no padding, URL-safe)
        assertThat(jwkKey.n()).doesNotContain("=");
        assertThat(jwkKey.e()).doesNotContain("=");
        assertThat(jwkKey.n()).doesNotContain("+");
        assertThat(jwkKey.e()).doesNotContain("+");
        assertThat(jwkKey.n()).doesNotContain("/");
        assertThat(jwkKey.e()).doesNotContain("/");
    }
}