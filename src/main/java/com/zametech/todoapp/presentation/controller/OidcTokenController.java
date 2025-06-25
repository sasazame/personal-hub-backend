package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.OidcTokenService;
import com.zametech.todoapp.presentation.dto.oidc.TokenRequest;
import com.zametech.todoapp.presentation.dto.oidc.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class OidcTokenController {
    
    private final OidcTokenService tokenService;
    
    @PostMapping(value = "/token", 
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            // Handle client authentication
            if (authorization != null && authorization.startsWith("Basic ")) {
                String credentials = new String(Base64.getDecoder().decode(
                    authorization.substring(6)), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    clientId = parts[0];
                    clientSecret = parts[1];
                }
            }
            
            TokenRequest request = TokenRequest.builder()
                .grantType(grantType)
                .code(code)
                .redirectUri(redirectUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .codeVerifier(codeVerifier)
                .refreshToken(refreshToken)
                .scope(scope)
                .build();
            
            TokenResponse response = tokenService.processTokenRequest(request);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid token request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error processing token request", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "server_error",
                "error_description", "Internal server error"
            ));
        }
    }
    
    /**
     * OAuth 2.0 Token Revocation Endpoint (RFC 7009)
     * Revokes access tokens or refresh tokens
     */
    @PostMapping(value = "/revoke", 
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            // Handle client authentication
            if (authorization != null && authorization.startsWith("Basic ")) {
                String credentials = new String(Base64.getDecoder().decode(
                    authorization.substring(6)), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    clientId = parts[0];
                    clientSecret = parts[1];
                }
            }
            
            // Validate required parameters
            if (token == null || token.trim().isEmpty()) {
                log.warn("Token revocation request missing token parameter");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "Missing required parameter: token"
                ));
            }
            
            // Revoke the token
            boolean revoked = tokenService.revokeToken(token, tokenTypeHint, clientId);
            
            if (revoked) {
                log.info("Token successfully revoked for client: {}", clientId);
                // RFC 7009: Return 200 OK for successful revocation
                return ResponseEntity.ok().build();
            } else {
                log.info("Token not found or already revoked for client: {}", clientId);
                // RFC 7009: Return 200 OK even if token was not found (for security)
                return ResponseEntity.ok().build();
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid token revocation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error processing token revocation request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "server_error",
                "error_description", "Internal server error"
            ));
        }
    }
}