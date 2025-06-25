package com.zametech.todoapp.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * トークン暗号化サービス
 * AES-GCM暗号化を使用してアクセストークンとリフレッシュトークンを安全に保存
 */
@Service
@Slf4j
public class TokenEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private final SecretKey secretKey;
    
    public TokenEncryptionService(@Value("${app.security.token-encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isEmpty()) {
            log.warn("Token encryption key not configured. Generating a new key (not recommended for production)");
            this.secretKey = generateKey();
        } else {
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
        }
    }
    
    /**
     * トークンを暗号化
     */
    public String encryptToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] encryptedToken = cipher.doFinal(token.getBytes());
            
            // Combine IV and encrypted token
            byte[] combined = new byte[iv.length + encryptedToken.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedToken, 0, combined, iv.length, encryptedToken.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Failed to encrypt token", e);
            throw new RuntimeException("Token encryption failed", e);
        }
    }
    
    /**
     * トークンを復号化
     */
    public String decryptToken(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return null;
        }
        
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedToken);
            
            // Extract IV and encrypted token
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] decryptedToken = cipher.doFinal(encryptedData);
            return new String(decryptedToken);
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("Token decryption failed - likely due to key change. User needs to re-authenticate.", e);
            return null; // Return null to trigger re-authentication
        } catch (Exception e) {
            log.error("Failed to decrypt token", e);
            return null; // Return null instead of throwing exception
        }
    }
    
    /**
     * 新しい暗号化キーを生成（開発用）
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();
            log.info("Generated encryption key (base64): {}", Base64.getEncoder().encodeToString(key.getEncoded()));
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}