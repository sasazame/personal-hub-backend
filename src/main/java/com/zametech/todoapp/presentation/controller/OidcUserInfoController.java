package com.zametech.todoapp.presentation.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.todoapp.application.service.OidcUserInfoService;
import com.zametech.todoapp.presentation.dto.oidc.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OidcUserInfoController {
    
    private final OidcUserInfoService userInfoService;
    
    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "invalid_token",
                    "error_description", "Bearer token required"
                ));
            }
            
            String accessToken = authorization.substring(7);
            
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            String scopeString = claims.getStringClaim("scope");
            List<String> scopes = scopeString != null ? 
                Arrays.asList(scopeString.split("\\s+")) : List.of();
            
            UserInfoResponse userInfo = userInfoService.getUserInfo(accessToken, scopes);
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            log.error("Error getting user info", e);
            return ResponseEntity.status(401).body(Map.of(
                "error", "invalid_token",
                "error_description", e.getMessage()
            ));
        }
    }
    
    @PostMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserInfoPost(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return getUserInfo(authorization);
    }
}