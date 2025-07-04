package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.TodoRepository;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.presentation.dto.request.ChangePasswordRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String CURRENT_PASSWORD = "currentPassword123!";
    private static final String NEW_PASSWORD = "newPassword123!";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void updateUserProfile_Success() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com",
                CURRENT_PASSWORD,
                NEW_PASSWORD
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfile(USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    void updateUserProfile_AccessDenied_DifferentUser() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com",
                CURRENT_PASSWORD,
                NEW_PASSWORD
        );

        when(userContextService.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only update your own profile");
    }

    @Test
    void updateUserProfile_InvalidCurrentPassword() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                "newusername",
                "newemail@example.com",
                "wrongPassword",
                NEW_PASSWORD
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid current password");
    }

    @Test
    void updateUserProfile_UsernameAlreadyExists() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                "existingusername",
                null,
                CURRENT_PASSWORD,
                null
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByUsername("existingusername")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void changePassword_Success() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(USER_ID, request);

        // Then
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(USER_ID);

        // Then
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void deleteUser_AccessDenied_DifferentUser() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only delete your own account");
    }

    @Test
    void getUserById_Success() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(USER_ID);

        // Then
        assertThat(result).isEqualTo(testUser);
    }

    @Test
    void getUserById_UserNotFound() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("User not found with id: " + USER_ID);
    }

    @Test
    void updateUserProfile_EmailAlreadyExists() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                "existing@example.com",
                CURRENT_PASSWORD,
                null
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void updateUserProfile_UserNotFound() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                null,
                CURRENT_PASSWORD,
                null
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(USER_ID, request))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("User not found with id: " + USER_ID);
    }

    @Test
    void changePassword_AccessDenied_DifferentUser() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);
        when(userContextService.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only change your own password");
    }

    @Test
    void changePassword_UserNotFound() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);
        
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("User not found with id: " + USER_ID);
    }

    @Test
    void changePassword_InvalidCurrentPassword() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", NEW_PASSWORD);

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid current password");
    }

    @Test
    void deleteUser_UserNotFound() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("User not found with id: " + USER_ID);
    }

    @Test
    void getUserById_AccessDenied_DifferentUser() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only view your own profile");
    }

    @Test
    void updateWeekStartDay_Success() {
        // Given
        Integer weekStartDay = 1; // Monday
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateWeekStartDay(USER_ID, weekStartDay);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(argThat(user -> user.getWeekStartDay().equals(weekStartDay)));
    }

    @Test
    void updateWeekStartDay_AccessDenied_DifferentUser() {
        // Given
        Integer weekStartDay = 1;
        when(userContextService.getCurrentUserId()).thenReturn(OTHER_USER_ID);

        // When & Then
        assertThatThrownBy(() -> userService.updateWeekStartDay(USER_ID, weekStartDay))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only update your own settings");
    }

    @Test
    void updateWeekStartDay_UserNotFound() {
        // Given
        Integer weekStartDay = 1;
        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateWeekStartDay(USER_ID, weekStartDay))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessage("User not found with id: " + USER_ID);
    }

    @Test
    void updateUserProfile_NoChanges() {
        // Given - request with nulls and same values
        UpdateUserRequest request = new UpdateUserRequest(
                testUser.getUsername(), // Same username
                testUser.getEmail(),    // Same email
                CURRENT_PASSWORD,
                null                    // No new password
        );

        when(userContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfile(USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }
}