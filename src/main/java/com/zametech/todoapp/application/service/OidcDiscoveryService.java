package com.zametech.todoapp.application.service;

import com.zametech.todoapp.presentation.dto.oidc.OidcDiscoveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OidcDiscoveryService {
    
    @Value("${app.oidc.issuer}")
    private String issuer;
    
    @Value("${app.oidc.base-url}")
    private String baseUrl;
    
    public OidcDiscoveryResponse getDiscoveryDocument() {
        return OidcDiscoveryResponse.builder()
            .issuer(issuer)
            .authorizationEndpoint(baseUrl + "/auth/authorize")
            .tokenEndpoint(baseUrl + "/auth/token")
            .userinfoEndpoint(baseUrl + "/auth/userinfo")
            .jwksUri(baseUrl + "/.well-known/jwks.json")
            .registrationEndpoint(baseUrl + "/auth/register")
            .scopesSupported(List.of("openid", "profile", "email", "offline_access"))
            .responseTypesSupported(List.of("code", "id_token", "token id_token"))
            .responseModesSupported(List.of("query", "fragment", "form_post"))
            .grantTypesSupported(List.of("authorization_code", "implicit", "refresh_token"))
            .acrValuesSupported(List.of())
            .subjectTypesSupported(List.of("public"))
            .idTokenSigningAlgValuesSupported(List.of("RS256", "ES256"))
            .idTokenEncryptionAlgValuesSupported(List.of())
            .idTokenEncryptionEncValuesSupported(List.of())
            .userinfoSigningAlgValuesSupported(List.of("RS256", "ES256"))
            .userinfoEncryptionAlgValuesSupported(List.of())
            .userinfoEncryptionEncValuesSupported(List.of())
            .requestObjectSigningAlgValuesSupported(List.of("RS256", "ES256"))
            .requestObjectEncryptionAlgValuesSupported(List.of())
            .requestObjectEncryptionEncValuesSupported(List.of())
            .tokenEndpointAuthMethodsSupported(List.of("client_secret_basic", "client_secret_post", "none"))
            .tokenEndpointAuthSigningAlgValuesSupported(List.of("RS256", "ES256"))
            .displayValuesSupported(List.of("page", "popup", "touch", "wap"))
            .claimTypesSupported(List.of("normal"))
            .claimsSupported(List.of(
                "sub", "name", "given_name", "family_name", "middle_name", "nickname",
                "preferred_username", "profile", "picture", "website", "email", "email_verified",
                "gender", "birthdate", "zoneinfo", "locale", "phone_number", "phone_number_verified",
                "address", "updated_at"
            ))
            .serviceDocumentation(baseUrl + "/docs")
            .claimsLocalesSupported(List.of("en-US", "ja-JP"))
            .uiLocalesSupported(List.of("en", "ja"))
            .claimsParameterSupported(true)
            .requestParameterSupported(true)
            .requestUriParameterSupported(false)
            .requireRequestUriRegistration(false)
            .opPolicyUri(baseUrl + "/policy")
            .opTosUri(baseUrl + "/terms")
            .codeChallengeMethodsSupported(List.of("plain", "S256"))
            .build();
    }
}