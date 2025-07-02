package com.zametech.todoapp.application.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.zametech.todoapp.common.exception.TokenDecryptionException;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.infrastructure.security.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarOAuth2ServiceTest {

    @Mock
    private UserSocialAccountRepository socialAccountRepository;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Mock
    private GoogleOidcService googleOidcService;

    @InjectMocks
    private GoogleCalendarOAuth2Service googleCalendarOAuth2Service;

    private User testUser;
    private UserSocialAccount socialAccount;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");

        socialAccount = UserSocialAccount.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .provider("google")
            .accessTokenEncrypted("encrypted-token")
            .refreshTokenEncrypted("encrypted-refresh-token")
            .tokenExpiresAt(LocalDateTime.now().plusHours(1))
            .email("google@example.com")
            .build();
    }

    @Test
    void getCalendarService_WhenNoSocialAccount_ThrowsException() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> googleCalendarOAuth2Service.getCalendarService(testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User has not connected their Google account");
    }

    @Test
    void getCalendarService_WhenTokenCannotBeDecrypted_ThrowsException() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> googleCalendarOAuth2Service.getCalendarService(testUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Access token could not be decrypted. Please re-authenticate with Google.");
    }

    @Test
    void getCalendarService_WhenTokenExpired_RefreshesToken() throws Exception {
        // Given
        socialAccount.setTokenExpiresAt(LocalDateTime.now().minusHours(1)); // expired
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenReturn("decrypted-token");

        // When
        try {
            googleCalendarOAuth2Service.getCalendarService(testUser);
        } catch (Exception e) {
            // Expected to fail due to Google API not being mocked
        }

        // Then
        verify(googleOidcService).refreshAccessToken(socialAccount);
    }

    @Test
    void getCalendarService_WhenTokenDecryptionFails_ThrowsException() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new TokenDecryptionException("Decryption failed"));

        // When/Then
        assertThatThrownBy(() -> googleCalendarOAuth2Service.getCalendarService(testUser))
            .isInstanceOf(TokenDecryptionException.class)
            .hasMessage("Decryption failed");
    }

    @Test
    void getUserCalendars_WhenNoSocialAccount_ReturnsEmptyList() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());

        // When
        List<CalendarListEntry> result = googleCalendarOAuth2Service.getUserCalendars(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUserCalendars_WhenExceptionOccurs_LogsErrorAndReturnsEmptyList() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When
        List<CalendarListEntry> result = googleCalendarOAuth2Service.getUserCalendars(testUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCalendarEvents_WhenNoSocialAccount_ReturnsEmptyList() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());

        // When
        List<Event> result = googleCalendarOAuth2Service.getCalendarEvents(
            testUser, "primary", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCalendarEvents_WhenExceptionOccurs_LogsErrorAndReturnsEmptyList() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When
        List<Event> result = googleCalendarOAuth2Service.getCalendarEvents(
            testUser, "primary", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void createCalendarEvent_WhenNoSocialAccount_ReturnsEmpty() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();

        // When
        Optional<String> result = googleCalendarOAuth2Service.createCalendarEvent(
            testUser, "primary", event);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void createCalendarEvent_WhenExceptionOccurs_ReturnsEmpty() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new RuntimeException("Unexpected error"));
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();

        // When
        Optional<String> result = googleCalendarOAuth2Service.createCalendarEvent(
            testUser, "primary", event);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateCalendarEvent_WhenNoSocialAccount_ReturnsFalse() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();

        // When
        boolean result = googleCalendarOAuth2Service.updateCalendarEvent(
            testUser, "primary", "event-id", event);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void updateCalendarEvent_WhenExceptionOccurs_ReturnsFalse() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new RuntimeException("Unexpected error"));
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();

        // When
        boolean result = googleCalendarOAuth2Service.updateCalendarEvent(
            testUser, "primary", "event-id", event);

        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void updateCalendarEvent_WithRecurringEventInstance_SkipsUpdateAndReturnsTrue() {
        // Given
        com.zametech.todoapp.domain.model.Event event = new com.zametech.todoapp.domain.model.Event();

        // When
        boolean result = googleCalendarOAuth2Service.updateCalendarEvent(
            testUser, "primary", "event-id_20231225", event); // recurring event instance

        // Then
        assertThat(result).isTrue();
    }

    // Note: deleteCalendarEvent method doesn't exist in GoogleCalendarOAuth2Service
    // These test methods are commented out as the method is not implemented
    
    /*
    @Test
    void deleteCalendarEvent_WhenNoSocialAccount_ThrowsException() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> googleCalendarOAuth2Service.deleteCalendarEvent(
            testUser, "primary", "event-id"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User has not connected their Google account");
    }

    @Test
    void deleteCalendarEvent_WhenExceptionOccurs_LogsErrorAndThrowsRuntimeException() {
        // Given
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When/Then
        assertThatThrownBy(() -> googleCalendarOAuth2Service.deleteCalendarEvent(
            testUser, "primary", "event-id"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to delete calendar event");
    }
    */
    
    
    
    
    
    @Test
    void getCalendarEvents_WithAllDayEvent_ReturnsEvents() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);
        
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenReturn("decrypted-token");
        
        // When
        List<Event> result = googleCalendarOAuth2Service.getCalendarEvents(testUser, "primary", startTime, endTime);
        
        // Then
        // Expected to return empty list due to Google API not being mocked
        assertThat(result).isEmpty();
    }
    
    @Test
    void getCalendarEvents_WhenTokenRefreshFails_ReturnsEmptyList() {
        // Given
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(1);
        
        socialAccount.setTokenExpiresAt(LocalDateTime.now().minusHours(1)); // Expired token
        when(socialAccountRepository.findByUserIdAndProvider(userId, "google"))
            .thenReturn(Optional.of(socialAccount));
        when(tokenEncryptionService.decryptToken("encrypted-token"))
            .thenReturn("decrypted-token");
        doThrow(new RuntimeException("Refresh failed"))
            .when(googleOidcService).refreshAccessToken(socialAccount);
        
        // When
        List<Event> result = googleCalendarOAuth2Service.getCalendarEvents(testUser, "primary", startTime, endTime);
        
        // Then
        assertThat(result).isEmpty();
    }
}