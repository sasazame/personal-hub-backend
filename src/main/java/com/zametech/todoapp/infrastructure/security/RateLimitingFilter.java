package com.zametech.todoapp.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to protect authentication endpoints from brute force attacks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${app.security.rate-limit.auth.capacity:10}")
    private int authCapacity;
    
    @Value("${app.security.rate-limit.auth.refill-tokens:10}")
    private int authRefillTokens;
    
    @Value("${app.security.rate-limit.auth.refill-period-minutes:15}")
    private int authRefillPeriodMinutes;
    
    @Value("${app.security.rate-limit.general.capacity:100}")
    private int generalCapacity;
    
    @Value("${app.security.rate-limit.general.refill-tokens:100}")
    private int generalRefillTokens;
    
    @Value("${app.security.rate-limit.general.refill-period-minutes:1}")
    private int generalRefillPeriodMinutes;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        
        // Different rate limits for authentication endpoints
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");
        
        String bucketKey = clientIp + ":" + (isAuthEndpoint ? "auth" : "general");
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(isAuthEndpoint));
        
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
        }
    }
    
    private Bucket createBucket(boolean isAuthEndpoint) {
        Bandwidth limit;
        
        if (isAuthEndpoint) {
            // Stricter limits for authentication endpoints
            limit = Bandwidth.classic(
                authCapacity, 
                Refill.intervally(authRefillTokens, Duration.ofMinutes(authRefillPeriodMinutes))
            );
        } else {
            // General API rate limits
            limit = Bandwidth.classic(
                generalCapacity,
                Refill.intervally(generalRefillTokens, Duration.ofMinutes(generalRefillPeriodMinutes))
            );
        }
        
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
    
    private String getClientIp(HttpServletRequest request) {
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