package com.zametech.todoapp.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import com.zametech.todoapp.presentation.dto.oidc.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcUserInfoServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OidcUserInfoService oidcUserInfoService;

    private User testUser;
    private UUID userId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        
        // This is a dummy token for testing - in real tests you'd create a proper signed JWT
        accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI" + userId.toString() + "iLCJpYXQiOjE1MTYyMzkwMjJ9.test";
    }

    @Test
    void getUserInfo_WithProfileScope_ReturnsProfileInfo() {
        // Given
        List<String> scopes = Arrays.asList("profile");
        testUser.setGivenName("Test");
        testUser.setFamilyName("User");
        testUser.setProfilePictureUrl("https://example.com/avatar.jpg");
        testUser.setLocale("en-US");
        testUser.setUpdatedAt(LocalDateTime.now());
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When
            UserInfoResponse response = oidcUserInfoService.getUserInfo(accessToken, scopes);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sub()).isEqualTo(userId.toString());
            assertThat(response.name()).isEqualTo("testuser");
            assertThat(response.preferredUsername()).isEqualTo("testuser");
            assertThat(response.givenName()).isEqualTo("Test");
            assertThat(response.familyName()).isEqualTo("User");
            assertThat(response.picture()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(response.locale()).isEqualTo("en-US");
            assertThat(response.updatedAt()).isNotNull();
            
            // Email should not be included without email scope
            assertThat(response.email()).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getUserInfo_WithEmailScope_ReturnsEmailInfo() {
        // Given
        List<String> scopes = Arrays.asList("email");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When
            UserInfoResponse response = oidcUserInfoService.getUserInfo(accessToken, scopes);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sub()).isEqualTo(userId.toString());
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.emailVerified()).isTrue();
            
            // Profile info should not be included without profile scope
            assertThat(response.name()).isNull();
            assertThat(response.preferredUsername()).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getUserInfo_WithBothScopes_ReturnsAllInfo() {
        // Given
        List<String> scopes = Arrays.asList("profile", "email");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When
            UserInfoResponse response = oidcUserInfoService.getUserInfo(accessToken, scopes);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sub()).isEqualTo(userId.toString());
            assertThat(response.name()).isEqualTo("testuser");
            assertThat(response.preferredUsername()).isEqualTo("testuser");
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.emailVerified()).isTrue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getUserInfo_UserNotFound_ThrowsException() {
        // Given
        List<String> scopes = Arrays.asList("profile", "email");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When/Then
            assertThatThrownBy(() -> oidcUserInfoService.getUserInfo(accessToken, scopes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get user info")
                .hasCauseInstanceOf(IllegalArgumentException.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getUserInfo_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid-token";
        List<String> scopes = Arrays.asList("profile");

        // Mock the static SignedJWT.parse method to throw exception
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            signedJWTMock.when(() -> SignedJWT.parse(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));

            // When/Then
            assertThatThrownBy(() -> oidcUserInfoService.getUserInfo(invalidToken, scopes))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get user info");
        }
    }

    @Test
    void getUserInfo_WithNullFields_HandlesGracefully() {
        // Given
        List<String> scopes = Arrays.asList("profile");
        testUser.setGivenName(null);
        testUser.setFamilyName(null);
        testUser.setProfilePictureUrl(null);
        testUser.setLocale(null);
        testUser.setUpdatedAt(null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When
            UserInfoResponse response = oidcUserInfoService.getUserInfo(accessToken, scopes);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sub()).isEqualTo(userId.toString());
            assertThat(response.name()).isEqualTo("testuser");
            assertThat(response.preferredUsername()).isEqualTo("testuser");
            assertThat(response.givenName()).isNull();
            assertThat(response.familyName()).isNull();
            assertThat(response.picture()).isNull();
            assertThat(response.locale()).isNull();
            assertThat(response.updatedAt()).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getUserInfo_WithEmptyScopes_ReturnsMinimalInfo() {
        // Given
        List<String> scopes = Arrays.asList();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Mock the static SignedJWT.parse method
        try (MockedStatic<SignedJWT> signedJWTMock = mockStatic(SignedJWT.class)) {
            SignedJWT mockJWT = mock(SignedJWT.class);
            JWTClaimsSet mockClaims = mock(JWTClaimsSet.class);
            
            signedJWTMock.when(() -> SignedJWT.parse(accessToken)).thenReturn(mockJWT);
            when(mockJWT.getJWTClaimsSet()).thenReturn(mockClaims);
            when(mockClaims.getSubject()).thenReturn(userId.toString());

            // When
            UserInfoResponse response = oidcUserInfoService.getUserInfo(accessToken, scopes);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sub()).isEqualTo(userId.toString());
            assertThat(response.name()).isNull();
            assertThat(response.email()).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}