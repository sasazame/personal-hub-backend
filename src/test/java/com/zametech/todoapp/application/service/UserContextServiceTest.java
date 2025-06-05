package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private UserContextService userContextService;

    @BeforeEach
    void setUp() {
        userContextService = new UserContextService(userRepository);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldReturnCurrentUserWhenAuthenticated() {
        User expectedUser = new User(
                1L,
                "test@example.com",
                "password",
                "testuser",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(expectedUser));

        User actualUser = userContextService.getCurrentUser();

        assertEquals(expectedUser, actualUser);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void shouldThrowExceptionWhenNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            userContextService.getCurrentUser();
        });
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            userContextService.getCurrentUser();
        });
    }

    @Test
    void shouldThrowExceptionWhenInvalidPrincipal() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("invalid");

        assertThrows(RuntimeException.class, () -> {
            userContextService.getCurrentUser();
        });
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            userContextService.getCurrentUser();
        });
    }

    @Test
    void shouldReturnCurrentUserId() {
        User user = new User(
                1L,
                "test@example.com",
                "password",
                "testuser",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Long userId = userContextService.getCurrentUserId();

        assertEquals(1L, userId);
    }

    @Test
    void shouldReturnTrueWhenUserIsCurrentUser() {
        User user = new User(
                1L,
                "test@example.com",
                "password",
                "testuser",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        boolean isCurrentUser = userContextService.isCurrentUser(1L);

        assertTrue(isCurrentUser);
    }

    @Test
    void shouldReturnFalseWhenUserIsNotCurrentUser() {
        User user = new User(
                1L,
                "test@example.com",
                "password",
                "testuser",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        boolean isCurrentUser = userContextService.isCurrentUser(2L);

        assertFalse(isCurrentUser);
    }
}