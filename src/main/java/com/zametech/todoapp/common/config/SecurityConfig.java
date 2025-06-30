package com.zametech.todoapp.common.config;

import com.zametech.todoapp.infrastructure.security.JwtAuthenticationFilter;
import com.zametech.todoapp.infrastructure.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * JWT認証を使用したセキュリティ設定
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/validate-reset-token").permitAll()
                .requestMatchers("/api/v1/auth/oidc/**").permitAll()
                .requestMatchers("/api/v1/.well-known/**").permitAll()
                .requestMatchers("/api/v1/oauth2/jwks").permitAll()
                .requestMatchers("/auth/token", "/auth/revoke").permitAll() // OAuth 2.0 endpoints
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/oauth2/userinfo").authenticated()
                .requestMatchers("/api/v1/todos/**").authenticated()
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/calendar/**").authenticated()
                .requestMatchers("/api/v1/notes/**").authenticated()
                .requestMatchers("/api/v1/events/**").authenticated()
                .requestMatchers("/api/v1/analytics/**").authenticated()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, LogoutFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}