package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import com.zametech.todoapp.infrastructure.security.JwtService;
import com.zametech.todoapp.presentation.dto.request.LoginRequest;
import com.zametech.todoapp.presentation.dto.request.RegisterRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUsername(request.getUsername());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("New user registered with email: {} and username: {}", savedUser.getEmail(), savedUser.getUsername());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .authorities(createDefaultAuthorities())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        UserResponse userResponse = new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );

        return new AuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.info("User logged in with email: {}", user.getEmail());

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(createDefaultAuthorities())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return new AuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    public AuthenticationResponse refreshToken(String refreshTokenString) {
        try {
            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshTokenString);
            
            if (username != null) {
                User user = userRepository.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Validate refresh token
                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword() != null ? user.getPassword() : "")
                        .authorities(createDefaultAuthorities())
                        .build();

                if (jwtService.isTokenValid(refreshTokenString, userDetails)) {
                    // Generate new tokens
                    String newAccessToken = jwtService.generateToken(userDetails);
                    String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                    UserResponse userResponse = new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getCreatedAt(),
                            user.getUpdatedAt()
                    );

                    log.info("Token refreshed for user: {}", user.getEmail());

                    return new AuthenticationResponse(newAccessToken, newRefreshToken, userResponse);
                }
            }
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Invalid refresh token");
        }

        throw new RuntimeException("Invalid refresh token");
    }

    /**
     * Creates default authorities for users
     */
    private List<SimpleGrantedAuthority> createDefaultAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }
}