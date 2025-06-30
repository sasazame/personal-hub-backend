package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.SecurityEvent;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.SecurityEventRepository;
import com.zametech.todoapp.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {

    @Mock
    private SecurityEventRepository securityEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest mockRequest;

    @InjectMocks
    private SecurityEventService securityEventService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        
        // Set @Value fields
        ReflectionTestUtils.setField(securityEventService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(securityEventService, "lockoutDurationMinutes", 30);
    }

    @Test
    void logSecurityEvent_withSuccess_shouldSaveEvent() {
        // Given
        setupMockRequest();
        String clientId = "web-app";
        
        // When
        securityEventService.logSecurityEvent(SecurityEvent.EventType.LOGIN_SUCCESS, testUser, clientId, true);
        
        // Then
        verify(securityEventRepository).save(any(SecurityEvent.class));
    }
    
    private void setupMockRequest() {
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);
        
        when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.100");
    }

    @Test
    void logSecurityEvent_withFailure_shouldSaveEventWithError() {
        // Given
        setupMockRequest();
        String clientId = "web-app";
        String errorCode = "INVALID_CREDENTIALS";
        String errorDescription = "Invalid username or password";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("attemptCount", 3);
        
        // When
        securityEventService.logSecurityEvent(
            SecurityEvent.EventType.LOGIN_FAILURE, 
            testUser, 
            clientId, 
            false,
            errorCode,
            errorDescription,
            metadata
        );
        
        // Then
        verify(securityEventRepository).save(argThat(event -> 
            event.getEventType() == SecurityEvent.EventType.LOGIN_FAILURE &&
            !event.getSuccess() &&
            event.getErrorCode().equals(errorCode) &&
            event.getErrorDescription().equals(errorDescription)
        ));
    }

    @Test
    void logSecurityEvent_withException_shouldNotThrow() {
        // Given
        RequestContextHolder.resetRequestAttributes(); // This will cause an exception
        
        // When/Then - should not throw
        securityEventService.logSecurityEvent(SecurityEvent.EventType.LOGIN_SUCCESS, testUser, "client", true);
    }

    @Test
    void isAccountLocked_withTooManyFailedAttempts_shouldReturnTrue() {
        // Given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        when(securityEventRepository.countFailedLoginAttempts(eq(userId), any(LocalDateTime.class)))
            .thenReturn(5L);
        
        // When
        boolean isLocked = securityEventService.isAccountLocked(userId);
        
        // Then
        assertThat(isLocked).isTrue();
    }

    @Test
    void isAccountLocked_withFewFailedAttempts_shouldReturnFalse() {
        // Given
        when(securityEventRepository.countFailedLoginAttempts(eq(userId), any(LocalDateTime.class)))
            .thenReturn(3L);
        
        // When
        boolean isLocked = securityEventService.isAccountLocked(userId);
        
        // Then
        assertThat(isLocked).isFalse();
    }

    @Test
    void recordLoginSuccess_shouldSaveSuccessEvent() {
        // Given
        String provider = "local";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loginMethod", "password");
        
        // When
        securityEventService.recordLoginSuccess(testUser, provider, ipAddress, userAgent, metadata);
        
        // Then
        verify(securityEventRepository).save(argThat(event ->
            event.getEventType() == SecurityEvent.EventType.LOGIN_SUCCESS &&
            event.getSuccess() &&
            event.getUser().equals(testUser) &&
            event.getClientId().equals(provider) &&
            event.getIpAddress().equals(ipAddress)
        ));
    }

    @Test
    void recordLoginFailure_shouldSaveFailureEvent() {
        // Given
        String provider = "local";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        String errorCode = "ACCOUNT_LOCKED";
        String errorDescription = "Account is locked due to multiple failed attempts";
        Map<String, Object> metadata = new HashMap<>();
        
        // When
        securityEventService.recordLoginFailure(testUser, provider, ipAddress, userAgent, 
            errorCode, errorDescription, metadata);
        
        // Then
        verify(securityEventRepository).save(argThat(event ->
            event.getEventType() == SecurityEvent.EventType.LOGIN_FAILURE &&
            !event.getSuccess() &&
            event.getErrorCode().equals(errorCode) &&
            event.getErrorDescription().equals(errorDescription)
        ));
    }

    @Test
    void isIpAddressLocked_withTooManyRecentFailures_shouldReturnTrue() {
        // Given
        String ipAddress = "192.168.1.100";
        
        // Simulate multiple failed attempts
        for (int i = 0; i < 5; i++) {
            securityEventService.trackFailedLoginAttempt(ipAddress);
        }
        
        // When
        boolean isLocked = securityEventService.isIpAddressLocked(ipAddress);
        
        // Then
        assertThat(isLocked).isTrue();
    }

    @Test
    void isIpAddressLocked_withOldFailures_shouldReturnFalse() {
        // Given
        String ipAddress = "192.168.1.100";
        
        // Directly manipulate the internal maps to simulate old failures
        Map<String, LocalDateTime> lastFailedAttemptTime = 
            (Map<String, LocalDateTime>) ReflectionTestUtils.getField(securityEventService, "lastFailedAttemptTime");
        Map<String, Integer> failedAttemptsPerIp = 
            (Map<String, Integer>) ReflectionTestUtils.getField(securityEventService, "failedAttemptsPerIp");
        
        lastFailedAttemptTime.put(ipAddress, LocalDateTime.now().minusHours(1));
        failedAttemptsPerIp.put(ipAddress, 10);
        
        // When
        boolean isLocked = securityEventService.isIpAddressLocked(ipAddress);
        
        // Then
        assertThat(isLocked).isFalse();
        assertThat(failedAttemptsPerIp).doesNotContainKey(ipAddress);
    }

    @Test
    void trackFailedLoginAttempt_shouldIncrementCounter() {
        // Given
        String ipAddress = "192.168.1.100";
        
        // When
        securityEventService.trackFailedLoginAttempt(ipAddress);
        securityEventService.trackFailedLoginAttempt(ipAddress);
        
        // Then
        Map<String, Integer> failedAttemptsPerIp = 
            (Map<String, Integer>) ReflectionTestUtils.getField(securityEventService, "failedAttemptsPerIp");
        assertThat(failedAttemptsPerIp.get(ipAddress)).isEqualTo(2);
    }

    @Test
    void clearFailedAttempts_shouldRemoveFromMaps() {
        // Given
        String ipAddress = "192.168.1.100";
        securityEventService.trackFailedLoginAttempt(ipAddress);
        
        // When
        securityEventService.clearFailedAttempts(ipAddress);
        
        // Then
        Map<String, Integer> failedAttemptsPerIp = 
            (Map<String, Integer>) ReflectionTestUtils.getField(securityEventService, "failedAttemptsPerIp");
        Map<String, LocalDateTime> lastFailedAttemptTime = 
            (Map<String, LocalDateTime>) ReflectionTestUtils.getField(securityEventService, "lastFailedAttemptTime");
        
        assertThat(failedAttemptsPerIp).doesNotContainKey(ipAddress);
        assertThat(lastFailedAttemptTime).doesNotContainKey(ipAddress);
    }

    @Test
    void recordTokenRefresh_shouldSaveTokenRefreshEvent() {
        // Given
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        
        // When
        securityEventService.recordTokenRefresh(testUser, ipAddress, userAgent);
        
        // Then
        verify(securityEventRepository).save(argThat(event ->
            event.getEventType() == SecurityEvent.EventType.TOKEN_REFRESH &&
            event.getSuccess() &&
            event.getUser().equals(testUser)
        ));
    }

    @Test
    void recordLogout_shouldSaveLogoutEvent() {
        // Given
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";
        
        // When
        securityEventService.recordLogout(testUser, ipAddress, userAgent);
        
        // Then
        verify(securityEventRepository).save(argThat(event ->
            event.getEventType() == SecurityEvent.EventType.LOGOUT &&
            event.getSuccess() &&
            event.getUser().equals(testUser)
        ));
    }

    @Test
    void getRecentSecurityEvents_shouldReturnEventsList() {
        // Given
        int limit = 10;
        List<SecurityEvent> expectedEvents = Arrays.asList(
            SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.LOGIN_SUCCESS)
                .user(testUser)
                .build(),
            SecurityEvent.builder()
                .eventType(SecurityEvent.EventType.TOKEN_REFRESH)
                .user(testUser)
                .build()
        );
        when(securityEventRepository.findRecentEventsByUser(userId, limit))
            .thenReturn(expectedEvents);
        
        // When
        List<SecurityEvent> events = securityEventService.getRecentSecurityEvents(userId, limit);
        
        // Then
        assertThat(events).hasSize(2);
        assertThat(events).isEqualTo(expectedEvents);
    }

    @Test
    void getSuspiciousActivitySummary_shouldReturnSummaryMap() {
        // Given
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        when(securityEventRepository.countByEventTypeAndSuccessAndCreatedAtAfter(
            eq(SecurityEvent.EventType.LOGIN_FAILURE), eq(false), any(LocalDateTime.class)))
            .thenReturn(15L);
        
        // Add some suspicious IPs
        for (int i = 0; i < 3; i++) {
            securityEventService.trackFailedLoginAttempt("192.168.1.101");
        }
        for (int i = 0; i < 4; i++) {
            securityEventService.trackFailedLoginAttempt("192.168.1.102");
        }
        
        // When
        Map<String, Object> summary = securityEventService.getSuspiciousActivitySummary();
        
        // Then
        assertThat(summary).containsKey("failedLoginsLast24h");
        assertThat(summary.get("failedLoginsLast24h")).isEqualTo(15L);
        assertThat(summary).containsKey("suspiciousIps");
        List<String> suspiciousIps = (List<String>) summary.get("suspiciousIps");
        assertThat(suspiciousIps).hasSize(2);
        assertThat(summary.get("lockedIpCount")).isEqualTo(2);
    }

}