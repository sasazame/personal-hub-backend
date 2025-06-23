package com.zametech.todoapp.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.model.UserSocialAccount;
import com.zametech.todoapp.domain.repository.UserRepository;
import com.zametech.todoapp.domain.repository.UserSocialAccountRepository;
import com.zametech.todoapp.presentation.dto.request.OidcCallbackRequest;
import com.zametech.todoapp.presentation.dto.response.AuthenticationResponse;
import com.zametech.todoapp.presentation.dto.response.GoogleUserInfo;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOidcService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String PROVIDER_NAME = "google";

    @Value("${google.client.id:}")
    private String clientId;

    @Value("${google.client.secret:}")
    private String clientSecret;

    @Value("${google.redirect.uri:}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final OidcTokenService tokenService;
    private final SecurityEventService securityEventService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Google認証URLを生成
     */
    public String generateAuthorizationUrl(String state, String nonce) {
        log.debug("Generating Google authorization URL with clientId: {}, redirectUri: {}", clientId, redirectUri);
        
        if (clientId == null || clientId.isEmpty()) {
            log.error("Google client ID is not configured. Please set GOOGLE_OIDC_CLIENT_ID environment variable.");
            throw new IllegalStateException("Google OAuth client ID is not configured");
        }
        
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    /**
     * Google OIDCコールバック処理
     */
    @Transactional
    public AuthenticationResponse handleCallback(OidcCallbackRequest request, String ipAddress, String userAgent) {
        try {
            // 1. 認可コードをアクセストークンに交換
            Map<String, Object> tokenResponse = exchangeCodeForToken(request.code());
            
            String accessToken = (String) tokenResponse.get("access_token");
            String idToken = (String) tokenResponse.get("id_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            
            // 2. IDトークンの検証（簡略版 - 本番環境では完全な検証が必要）
            // TODO: Implement proper ID token validation
            
            // 3. ユーザー情報を取得
            GoogleUserInfo userInfo = fetchUserInfo(accessToken);
            
            // 4. ユーザーアカウントを作成または更新
            User user = findOrCreateUser(userInfo);
            
            // 5. ソーシャルアカウント情報を保存
            saveSocialAccount(user, userInfo, accessToken, refreshToken);
            
            // 6. JWTトークンを生成
            String jwtToken = tokenService.generateToken(user);
            
            // 7. セキュリティイベントを記録
            securityEventService.recordLoginSuccess(user, PROVIDER_NAME, ipAddress, userAgent, null);
            
            log.info("Google OIDC authentication successful for user: {}", user.getEmail());
            
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
            
            return new AuthenticationResponse(
                    jwtToken,
                    null, // No refresh token for OIDC login
                    userResponse
            );
            
        } catch (Exception e) {
            log.error("Google OIDC authentication failed", e);
            securityEventService.recordLoginFailure(null, PROVIDER_NAME, ipAddress, userAgent, 
                    "GOOGLE_AUTH_FAILED", e.getMessage(), null);
            throw new RuntimeException("Google authentication failed", e);
        }
    }

    /**
     * 認可コードをトークンに交換
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to exchange code for token");
            
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange authorization code", e);
        }
    }

    /**
     * Googleユーザー情報を取得
     */
    private GoogleUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    request,
                    GoogleUserInfo.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to fetch user info");
            
        } catch (Exception e) {
            log.error("Error fetching Google user info", e);
            throw new RuntimeException("Failed to fetch user information", e);
        }
    }

    /**
     * ユーザーを検索または作成
     */
    private User findOrCreateUser(GoogleUserInfo userInfo) {
        // まずソーシャルアカウントで検索
        Optional<UserSocialAccount> socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(PROVIDER_NAME, userInfo.sub());
        
        if (socialAccount.isPresent()) {
            return socialAccount.get().getUser();
        }
        
        // メールアドレスで既存ユーザーを検索
        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());
        
        if (existingUser.isPresent()) {
            // 既存ユーザーにGoogleアカウントをリンク
            User user = existingUser.get();
            updateUserFromGoogle(user, userInfo);
            return userRepository.save(user);
        }
        
        // 新規ユーザーを作成
        return createNewUser(userInfo);
    }

    /**
     * 新規ユーザーを作成
     */
    private User createNewUser(GoogleUserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.email());
        user.setEmailVerified(userInfo.emailVerified());
        user.setUsername(generateUsername(userInfo));
        user.setGivenName(userInfo.givenName());
        user.setFamilyName(userInfo.familyName());
        user.setProfilePictureUrl(userInfo.picture());
        user.setLocale(userInfo.locale());
        
        // Googleユーザーはパスワードなし
        user.setPassword(null);
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * 既存ユーザー情報をGoogleから更新
     */
    private void updateUserFromGoogle(User user, GoogleUserInfo userInfo) {
        if (!user.isEmailVerified() && userInfo.emailVerified()) {
            user.setEmailVerified(true);
        }
        
        if (user.getGivenName() == null && userInfo.givenName() != null) {
            user.setGivenName(userInfo.givenName());
        }
        
        if (user.getFamilyName() == null && userInfo.familyName() != null) {
            user.setFamilyName(userInfo.familyName());
        }
        
        if (user.getProfilePictureUrl() == null && userInfo.picture() != null) {
            user.setProfilePictureUrl(userInfo.picture());
        }
        
        if (user.getLocale() == null && userInfo.locale() != null) {
            user.setLocale(userInfo.locale());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * ソーシャルアカウント情報を保存
     */
    private void saveSocialAccount(User user, GoogleUserInfo userInfo, String accessToken, String refreshToken) {
        Optional<UserSocialAccount> existing = socialAccountRepository
                .findByUserIdAndProvider(user.getId(), PROVIDER_NAME);
        
        UserSocialAccount socialAccount;
        if (existing.isPresent()) {
            socialAccount = existing.get();
            socialAccount.setUpdatedAt(LocalDateTime.now());
        } else {
            socialAccount = UserSocialAccount.builder()
                    .user(user)
                    .provider(PROVIDER_NAME)
                    .providerUserId(userInfo.sub())
                    .email(userInfo.email())
                    .emailVerified(userInfo.emailVerified())
                    .name(userInfo.name())
                    .givenName(userInfo.givenName())
                    .familyName(userInfo.familyName())
                    .picture(userInfo.picture())
                    .locale(userInfo.locale())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        // プロファイルデータを保存
        socialAccount.setProfileData(objectMapper.convertValue(userInfo, Map.class));
        
        // トークンを暗号化して保存（TODO: 実装）
        // socialAccount.setAccessTokenEncrypted(encryptToken(accessToken));
        // socialAccount.setRefreshTokenEncrypted(encryptToken(refreshToken));
        
        socialAccountRepository.save(socialAccount);
    }

    /**
     * ユーザー名を生成
     */
    private String generateUsername(GoogleUserInfo userInfo) {
        String base = userInfo.email().split("@")[0];
        String username = base;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        
        return username;
    }
}