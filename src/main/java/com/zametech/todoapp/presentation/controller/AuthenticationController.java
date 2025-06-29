package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.AuthenticationService;
import com.zametech.todoapp.application.service.PasswordResetService;
import com.zametech.todoapp.application.service.UserContextService;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.presentation.dto.request.ForgotPasswordRequest;
import com.zametech.todoapp.presentation.dto.request.LoginRequest;
import com.zametech.todoapp.presentation.dto.request.RefreshTokenRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import com.zametech.todoapp.presentation.dto.request.ResetPasswordRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.PasswordResetResponse;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserContextService userContextService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthenticationResponse response = authenticationService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User currentUser = userContextService.getCurrentUser();
        
        UserResponse userResponse = new UserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getWeekStartDay(),
                currentUser.getCreatedAt(),
                currentUser.getUpdatedAt()
        );
        
        return ResponseEntity.ok(userResponse);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // Since JWTs are stateless, logout is handled on the client side
        // by removing the token from storage.
        // In a production system, you might want to maintain a blacklist
        // of revoked tokens for additional security.
        
        return ResponseEntity.ok(Map.of(
            "message", "Logout successful",
            "note", "Please remove the token from client storage"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        try {
            passwordResetService.requestPasswordReset(request.email());
            return ResponseEntity.ok(new PasswordResetResponse(
                "If an account with this email exists, you will receive a password reset email.",
                true
            ));
        } catch (Exception e) {
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(new PasswordResetResponse(
                "If an account with this email exists, you will receive a password reset email.",
                true
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new PasswordResetResponse(
            "Password has been successfully reset. You can now login with your new password.",
            true
        ));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<PasswordResetResponse> validateResetToken(
            @RequestParam String token
    ) {
        passwordResetService.validateToken(token);
        return ResponseEntity.ok(new PasswordResetResponse(
            "Token is valid",
            true
        ));
    }
}