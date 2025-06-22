package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.GoogleOidcService;
import com.zametech.todoapp.presentation.dto.request.OidcCallbackRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.OidcAuthorizationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oidc")
@RequiredArgsConstructor
public class OidcController {

    private final GoogleOidcService googleOidcService;

    /**
     * Google OIDC認証開始
     */
    @GetMapping("/google/authorize")
    public ResponseEntity<OidcAuthorizationResponse> initiateGoogleAuth() {
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        
        // TODO: stateとnonceをセッションまたはキャッシュに保存
        
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
        
        // TODO: stateパラメータの検証
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthenticationResponse response = googleOidcService.handleCallback(request, ipAddress, userAgent);
        
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