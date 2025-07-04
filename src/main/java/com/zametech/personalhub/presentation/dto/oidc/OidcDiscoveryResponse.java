package com.zametech.personalhub.presentation.dto.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record OidcDiscoveryResponse(
    @JsonProperty("issuer")
    String issuer,
    
    @JsonProperty("authorization_endpoint")
    String authorizationEndpoint,
    
    @JsonProperty("token_endpoint")
    String tokenEndpoint,
    
    @JsonProperty("userinfo_endpoint")
    String userinfoEndpoint,
    
    @JsonProperty("jwks_uri")
    String jwksUri,
    
    @JsonProperty("registration_endpoint")
    String registrationEndpoint,
    
    @JsonProperty("scopes_supported")
    List<String> scopesSupported,
    
    @JsonProperty("response_types_supported")
    List<String> responseTypesSupported,
    
    @JsonProperty("response_modes_supported")
    List<String> responseModesSupported,
    
    @JsonProperty("grant_types_supported")
    List<String> grantTypesSupported,
    
    @JsonProperty("acr_values_supported")
    List<String> acrValuesSupported,
    
    @JsonProperty("subject_types_supported")
    List<String> subjectTypesSupported,
    
    @JsonProperty("id_token_signing_alg_values_supported")
    List<String> idTokenSigningAlgValuesSupported,
    
    @JsonProperty("id_token_encryption_alg_values_supported")
    List<String> idTokenEncryptionAlgValuesSupported,
    
    @JsonProperty("id_token_encryption_enc_values_supported")
    List<String> idTokenEncryptionEncValuesSupported,
    
    @JsonProperty("userinfo_signing_alg_values_supported")
    List<String> userinfoSigningAlgValuesSupported,
    
    @JsonProperty("userinfo_encryption_alg_values_supported")
    List<String> userinfoEncryptionAlgValuesSupported,
    
    @JsonProperty("userinfo_encryption_enc_values_supported")
    List<String> userinfoEncryptionEncValuesSupported,
    
    @JsonProperty("request_object_signing_alg_values_supported")
    List<String> requestObjectSigningAlgValuesSupported,
    
    @JsonProperty("request_object_encryption_alg_values_supported")
    List<String> requestObjectEncryptionAlgValuesSupported,
    
    @JsonProperty("request_object_encryption_enc_values_supported")
    List<String> requestObjectEncryptionEncValuesSupported,
    
    @JsonProperty("token_endpoint_auth_methods_supported")
    List<String> tokenEndpointAuthMethodsSupported,
    
    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    List<String> tokenEndpointAuthSigningAlgValuesSupported,
    
    @JsonProperty("display_values_supported")
    List<String> displayValuesSupported,
    
    @JsonProperty("claim_types_supported")
    List<String> claimTypesSupported,
    
    @JsonProperty("claims_supported")
    List<String> claimsSupported,
    
    @JsonProperty("service_documentation")
    String serviceDocumentation,
    
    @JsonProperty("claims_locales_supported")
    List<String> claimsLocalesSupported,
    
    @JsonProperty("ui_locales_supported")
    List<String> uiLocalesSupported,
    
    @JsonProperty("claims_parameter_supported")
    Boolean claimsParameterSupported,
    
    @JsonProperty("request_parameter_supported")
    Boolean requestParameterSupported,
    
    @JsonProperty("request_uri_parameter_supported")
    Boolean requestUriParameterSupported,
    
    @JsonProperty("require_request_uri_registration")
    Boolean requireRequestUriRegistration,
    
    @JsonProperty("op_policy_uri")
    String opPolicyUri,
    
    @JsonProperty("op_tos_uri")
    String opTosUri,
    
    @JsonProperty("code_challenge_methods_supported")
    List<String> codeChallengeMethodsSupported
) {}