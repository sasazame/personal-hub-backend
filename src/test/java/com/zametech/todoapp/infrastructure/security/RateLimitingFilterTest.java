package com.zametech.todoapp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private PrintWriter writer;
    
    private RateLimitingFilter rateLimitingFilter;
    
    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
        
        // Set rate limit configuration
        ReflectionTestUtils.setField(rateLimitingFilter, "authCapacity", 5);
        ReflectionTestUtils.setField(rateLimitingFilter, "authRefillTokens", 5);
        ReflectionTestUtils.setField(rateLimitingFilter, "authRefillPeriodMinutes", 15);
        ReflectionTestUtils.setField(rateLimitingFilter, "generalCapacity", 10);
        ReflectionTestUtils.setField(rateLimitingFilter, "generalRefillTokens", 10);
        ReflectionTestUtils.setField(rateLimitingFilter, "generalRefillPeriodMinutes", 1);
    }
    
    @Test
    void testRateLimitingAllowsRequestsWithinLimit() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/v1/todos");
        
        // Act - Make requests within limit
        for (int i = 0; i < 10; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    @Test
    void testRateLimitingBlocksExcessiveRequests() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/v1/todos");
        when(response.getWriter()).thenReturn(writer);
        
        // Act - Make requests exceeding limit
        for (int i = 0; i < 15; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, atLeast(5)).setStatus(429);
        verify(response, atLeast(5)).setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
    }
    
    @Test
    void testAuthEndpointHasStricterLimit() throws Exception {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(response.getWriter()).thenReturn(writer);
        
        // Act - Make requests to auth endpoint
        for (int i = 0; i < 8; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Assert
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, atLeast(3)).setStatus(429);
    }
    
    @Test
    void testDifferentIPsHaveSeparateLimits() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/todos");
        
        // Act - Make requests from different IPs
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        for (int i = 0; i < 10; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        for (int i = 0; i < 10; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
        
        // Assert
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}