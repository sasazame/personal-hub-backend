package com.zametech.todoapp.infrastructure.security;

import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedPassword");
        testUser.setEnabled(true);
    }

    @Test
    void loadUserByUsername_WithValidEmail_ReturnsUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsername_WithOAuthUser_ReturnsDummyPassword() {
        // Given
        testUser.setEmail("oauth@example.com");
        testUser.setPassword(null); // OAuth user with no password
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("oauth@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("oauth@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$dummypasswordthatwillnevermatchanyuserinput");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_WithDisabledUser_ReturnsDisabledUserDetails() {
        // Given
        testUser.setEmail("disabled@example.com");
        testUser.setEnabled(false);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("disabled@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("disabled@example.com");
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_WithNonExistentEmail_ThrowsUsernameNotFoundException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Invalid email or password");
    }
}