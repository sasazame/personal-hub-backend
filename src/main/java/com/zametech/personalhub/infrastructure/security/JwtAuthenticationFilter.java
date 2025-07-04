package com.zametech.personalhub.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.stream.Collectors;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        log.debug("Processing JWT for request: {}", request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found, passing through");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Extract authorities from JWT token
                    List<String> authoritiesFromToken = jwtService.extractAuthorities(jwt);
                    List<SimpleGrantedAuthority> authorities = authoritiesFromToken != null 
                        ? authoritiesFromToken.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
                        : userDetails.getAuthorities().stream()
                            .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authentication successful for user: {}", userEmail);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // JWT has expired - log and continue without authentication
            log.warn("JWT token expired for request: {} - User needs to re-authenticate", request.getRequestURI());
            // Don't set authentication context - user will need to re-authenticate
        } catch (io.jsonwebtoken.JwtException e) {
            // Invalid JWT - log and continue without authentication
            log.warn("Invalid JWT token for request: {} - Error: {}", request.getRequestURI(), e.getMessage());
        } catch (Exception e) {
            // Any other exception - log and continue without authentication
            log.warn("Error processing JWT token for request: {} - Error: {}", request.getRequestURI(), e.getMessage());
            if (e.getCause() != null) {
                log.debug("Root cause: {}", e.getCause().getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}