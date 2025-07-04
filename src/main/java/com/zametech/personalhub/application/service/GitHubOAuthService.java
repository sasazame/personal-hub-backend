package com.zametech.personalhub.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.model.UserSocialAccount;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.domain.repository.UserSocialAccountRepository;
import com.zametech.personalhub.presentation.dto.request.OidcCallbackRequest;
import com.zametech.personalhub.presentation.dto.response.AuthenticationResponse;
import com.zametech.personalhub.presentation.dto.response.UserResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_USER_EMAILS_URL = "https://api.github.com/user/emails";
    private static final String PROVIDER_NAME = "github";

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final OidcTokenService tokenService;
    private final SecurityEventService securityEventService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * GitHub認証URLを生成
     */
    public String generateAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(GITHUB_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * GitHub OAuthコールバック処理
     */
    @Transactional
    public AuthenticationResponse handleCallback(OidcCallbackRequest request, String ipAddress, String userAgent) {
        try {
            // 1. 認可コードをアクセストークンに交換
            String accessToken = exchangeCodeForToken(request.code());
            
            // 2. ユーザー情報を取得
            Map<String, Object> userInfo = fetchUserInfo(accessToken);
            Map<String, Object> primaryEmail = fetchPrimaryEmail(accessToken);
            
            // 3. ユーザーアカウントを作成または更新
            User user = findOrCreateUser(userInfo, primaryEmail);
            
            // 4. ソーシャルアカウント情報を保存
            saveSocialAccount(user, userInfo, primaryEmail, accessToken);
            
            // 5. JWTトークンを生成
            String jwtToken = tokenService.generateToken(user);
            
            // 6. セキュリティイベントを記録
            securityEventService.recordLoginSuccess(user, PROVIDER_NAME, ipAddress, userAgent, null);
            
            log.info("GitHub OAuth authentication successful for user: {}", user.getEmail());
            
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getWeekStartDay(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
            
            return new AuthenticationResponse(
                    jwtToken,
                    null, // No refresh token for OAuth login
                    userResponse
            );
            
        } catch (Exception e) {
            log.error("GitHub OAuth authentication failed", e);
            securityEventService.recordLoginFailure(null, PROVIDER_NAME, ipAddress, userAgent, 
                    "GITHUB_AUTH_FAILED", e.getMessage(), null);
            throw new RuntimeException("GitHub authentication failed", e);
        }
    }

    /**
     * 認可コードをアクセストークンに交換
     */
    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    GITHUB_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode body = response.getBody();
                if (body.has("access_token")) {
                    return body.get("access_token").asText();
                } else if (body.has("error")) {
                    String error = body.get("error").asText();
                    String errorDescription = body.has("error_description") ? 
                            body.get("error_description").asText() : "Unknown error";
                    throw new RuntimeException("GitHub token exchange failed: " + error + " - " + errorDescription);
                }
            }
            
            throw new RuntimeException("Failed to exchange code for token");
            
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange authorization code", e);
        }
    }

    /**
     * GitHubユーザー情報を取得
     */
    private Map<String, Object> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GITHUB_USER_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to fetch user info");
            
        } catch (Exception e) {
            log.error("Error fetching GitHub user info", e);
            throw new RuntimeException("Failed to fetch user information", e);
        }
    }

    /**
     * GitHubプライマリメールアドレスを取得
     */
    private Map<String, Object> fetchPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    GITHUB_USER_EMAILS_URL,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode emails = response.getBody();
                for (JsonNode email : emails) {
                    if (email.get("primary").asBoolean()) {
                        Map<String, Object> emailInfo = new HashMap<>();
                        emailInfo.put("email", email.get("email").asText());
                        emailInfo.put("verified", email.get("verified").asBoolean());
                        return emailInfo;
                    }
                }
            }
            
            throw new RuntimeException("No primary email found");
            
        } catch (Exception e) {
            log.error("Error fetching GitHub user emails", e);
            throw new RuntimeException("Failed to fetch user email", e);
        }
    }

    /**
     * ユーザーを検索または作成
     */
    private User findOrCreateUser(Map<String, Object> userInfo, Map<String, Object> emailInfo) {
        String githubId = String.valueOf(userInfo.get("id"));
        String email = (String) emailInfo.get("email");
        boolean emailVerified = (boolean) emailInfo.get("verified");
        
        // まずソーシャルアカウントで検索
        Optional<UserSocialAccount> socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(PROVIDER_NAME, githubId);
        
        if (socialAccount.isPresent()) {
            return socialAccount.get().getUser();
        }
        
        // メールアドレスで既存ユーザーを検索
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            // 既存ユーザーにGitHubアカウントをリンク
            User user = existingUser.get();
            updateUserFromGitHub(user, userInfo, emailInfo);
            return userRepository.save(user);
        }
        
        // 新規ユーザーを作成
        return createNewUser(userInfo, emailInfo);
    }

    /**
     * 新規ユーザーを作成
     */
    private User createNewUser(Map<String, Object> userInfo, Map<String, Object> emailInfo) {
        User user = new User();
        user.setEmail((String) emailInfo.get("email"));
        user.setEmailVerified((boolean) emailInfo.get("verified"));
        user.setUsername(generateUsername(userInfo));
        
        // GitHubプロフィール情報を設定
        if (userInfo.get("name") != null) {
            String fullName = (String) userInfo.get("name");
            String[] nameParts = fullName.split(" ", 2);
            if (nameParts.length > 0) {
                user.setGivenName(nameParts[0]);
            }
            if (nameParts.length > 1) {
                user.setFamilyName(nameParts[1]);
            }
        }
        
        if (userInfo.get("avatar_url") != null) {
            user.setProfilePictureUrl((String) userInfo.get("avatar_url"));
        }
        
        // GitHubユーザーはパスワードなし
        user.setPassword(null);
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * 既存ユーザー情報をGitHubから更新
     */
    private void updateUserFromGitHub(User user, Map<String, Object> userInfo, Map<String, Object> emailInfo) {
        if (!user.getEmailVerified() && (boolean) emailInfo.get("verified")) {
            user.setEmailVerified(true);
        }
        
        if (user.getGivenName() == null && userInfo.get("name") != null) {
            String fullName = (String) userInfo.get("name");
            String[] nameParts = fullName.split(" ", 2);
            if (nameParts.length > 0) {
                user.setGivenName(nameParts[0]);
            }
            if (nameParts.length > 1) {
                user.setFamilyName(nameParts[1]);
            }
        }
        
        if (user.getProfilePictureUrl() == null && userInfo.get("avatar_url") != null) {
            user.setProfilePictureUrl((String) userInfo.get("avatar_url"));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * ソーシャルアカウント情報を保存
     */
    private void saveSocialAccount(User user, Map<String, Object> userInfo, Map<String, Object> emailInfo, String accessToken) {
        String githubId = String.valueOf(userInfo.get("id"));
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
                    .providerUserId(githubId)
                    .email((String) emailInfo.get("email"))
                    .emailVerified((boolean) emailInfo.get("verified"))
                    .name((String) userInfo.get("name"))
                    .picture((String) userInfo.get("avatar_url"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        
        // プロファイルデータを保存
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("login", userInfo.get("login"));
        profileData.put("id", userInfo.get("id"));
        profileData.put("node_id", userInfo.get("node_id"));
        profileData.put("avatar_url", userInfo.get("avatar_url"));
        profileData.put("html_url", userInfo.get("html_url"));
        profileData.put("name", userInfo.get("name"));
        profileData.put("company", userInfo.get("company"));
        profileData.put("blog", userInfo.get("blog"));
        profileData.put("location", userInfo.get("location"));
        profileData.put("bio", userInfo.get("bio"));
        profileData.put("public_repos", userInfo.get("public_repos"));
        profileData.put("followers", userInfo.get("followers"));
        profileData.put("following", userInfo.get("following"));
        socialAccount.setProfileData(profileData);
        
        // トークンを暗号化して保存（TODO: 実装）
        // socialAccount.setAccessTokenEncrypted(encryptToken(accessToken));
        
        socialAccountRepository.save(socialAccount);
    }

    /**
     * ユーザー名を生成
     */
    private String generateUsername(Map<String, Object> userInfo) {
        String login = (String) userInfo.get("login");
        String username = login;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = login + counter++;
        }
        
        return username;
    }
}