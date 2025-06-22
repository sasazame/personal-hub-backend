package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.OidcAuthorizationService;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.presentation.dto.oidc.AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class OidcAuthorizationController {
    
    private final OidcAuthorizationService authorizationService;
    
    @GetMapping("/authorize")
    public RedirectView authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam(value = "display", required = false) String display,
            @RequestParam(value = "max_age", required = false) String maxAge,
            @RequestParam(value = "ui_locales", required = false) String uiLocales,
            @RequestParam(value = "id_token_hint", required = false) String idTokenHint,
            @RequestParam(value = "login_hint", required = false) String loginHint,
            @RequestParam(value = "acr_values", required = false) String acrValues,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            HttpServletRequest request,
            Principal principal) {
        
        try {
            AuthorizationRequest authRequest = AuthorizationRequest.fromQueryParams(
                responseType, clientId, redirectUri, scope, state, nonce, prompt, display,
                maxAge, uiLocales, idTokenHint, loginHint, acrValues, codeChallenge, codeChallengeMethod
            );
            
            HttpSession session = request.getSession();
            session.setAttribute("authorization_request", authRequest);
            
            if (principal == null) {
                String loginUrl = "/login?redirect_uri=" + URLEncoder.encode(request.getRequestURL().toString() + "?" + request.getQueryString(), StandardCharsets.UTF_8);
                return new RedirectView(loginUrl);
            }
            
            // Get current user from authentication
            User currentUser = getCurrentUser(principal);
            
            String authorizationCode = authorizationService.generateAuthorizationCode(authRequest, currentUser);
            
            String redirectUrl = buildRedirectUrl(redirectUri, authorizationCode, state);
            
            return new RedirectView(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error in authorization flow", e);
            String errorRedirectUrl = buildErrorRedirectUrl(redirectUri, "server_error", e.getMessage(), state);
            return new RedirectView(errorRedirectUrl);
        }
    }
    
    @PostMapping("/authorize")
    public ResponseEntity<?> authorizePost(@Valid @RequestBody AuthorizationRequest request, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            
            User currentUser = getCurrentUser(principal);
            String authorizationCode = authorizationService.generateAuthorizationCode(request, currentUser);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "code", authorizationCode,
                "state", request.state() != null ? request.state() : ""
            ));
            
        } catch (Exception e) {
            log.error("Error in authorization flow", e);
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", "invalid_request",
                "error_description", e.getMessage()
            ));
        }
    }
    
    private User getCurrentUser(Principal principal) {
        // This is a simplified implementation. In a real application,
        // you would fetch the user from the database using the principal
        User user = new User();
        user.setUsername(principal.getName());
        user.setEmail(principal.getName() + "@example.com");
        user.setEmailVerified(true);
        user.setEnabled(true);
        return user;
    }
    
    private String buildRedirectUrl(String redirectUri, String code, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
        if (state != null) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return url.toString();
    }
    
    private String buildErrorRedirectUrl(String redirectUri, String error, String errorDescription, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        if (errorDescription != null) {
            url.append("&error_description=").append(URLEncoder.encode(errorDescription, StandardCharsets.UTF_8));
        }
        if (state != null) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return url.toString();
    }
}