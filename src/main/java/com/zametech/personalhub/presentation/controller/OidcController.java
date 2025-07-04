package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.GitHubOAuthService;
import com.zametech.personalhub.application.service.GoogleOidcService;
import com.zametech.personalhub.application.service.OAuthStateService;
import com.zametech.personalhub.presentation.dto.request.OidcCallbackRequest;
import com.zametech.personalhub.presentation.dto.response.AuthenticationResponse;
import com.zametech.personalhub.presentation.dto.response.OidcAuthorizationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oidc")
@RequiredArgsConstructor
public class OidcController {

    private final GoogleOidcService googleOidcService;
    private final GitHubOAuthService gitHubOAuthService;
    private final OAuthStateService oAuthStateService;

    /**
     * Google OIDC認証開始
     */
    @GetMapping("/google/authorize")
    public ResponseEntity<OidcAuthorizationResponse> initiateGoogleAuth() {
        String state = oAuthStateService.generateState("google");
        String nonce = UUID.randomUUID().toString();
        
        String authorizationUrl = googleOidcService.generateAuthorizationUrl(state, nonce);
        
        log.info("Initiating Google OIDC authentication with state: {}", state);
        
        return ResponseEntity.ok(new OidcAuthorizationResponse(
                authorizationUrl,
                state,
                "google"
        ));
    }

    /**
     * Google OIDCコールバック
     */
    @PostMapping("/google/callback")
    public ResponseEntity<AuthenticationResponse> handleGoogleCallback(
            @Valid @RequestBody OidcCallbackRequest request,
            HttpServletRequest httpRequest) {
        
        // エラーチェック
        if (request.error() != null) {
            log.error("Google OIDC callback error: {} - {}", request.error(), request.errorDescription());
            return ResponseEntity.badRequest().build();
        }
        
        // stateパラメータの検証
        String provider = oAuthStateService.validateStateAndGetProvider(request.state());
        if (provider == null || !"google".equals(provider)) {
            log.error("Invalid OAuth state for Google callback: {}", request.state());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResponse response = googleOidcService.handleCallback(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GitHub OAuth認証開始
     */
    @GetMapping("/github/authorize")
    public ResponseEntity<OidcAuthorizationResponse> initiateGitHubAuth() {
        String state = oAuthStateService.generateState("github");
        
        String authorizationUrl = gitHubOAuthService.generateAuthorizationUrl(state);
        
        log.info("Initiating GitHub OAuth authentication with state: {}", state);
        
        return ResponseEntity.ok(new OidcAuthorizationResponse(
                authorizationUrl,
                state,
                "github"
        ));
    }

    /**
     * GitHub OAuthコールバック
     */
    @PostMapping("/github/callback")
    public ResponseEntity<AuthenticationResponse> handleGitHubCallback(
            @Valid @RequestBody OidcCallbackRequest request,
            HttpServletRequest httpRequest) {
        
        // エラーチェック
        if (request.error() != null) {
            log.error("GitHub OAuth callback error: {} - {}", request.error(), request.errorDescription());
            return ResponseEntity.badRequest().build();
        }
        
        // stateパラメータの検証
        String provider = oAuthStateService.validateStateAndGetProvider(request.state());
        if (provider == null || !"github".equals(provider)) {
            log.error("Invalid OAuth state for GitHub callback: {}", request.state());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResponse response = gitHubOAuthService.handleCallback(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(response);
    }

    /**
     * クライアントIPアドレスを取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}