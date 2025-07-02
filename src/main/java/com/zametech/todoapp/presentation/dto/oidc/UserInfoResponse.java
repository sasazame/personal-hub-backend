package com.zametech.todoapp.presentation.dto.oidc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponse(
    String sub,
    String name,
    @JsonProperty("given_name") String givenName,
    @JsonProperty("family_name") String familyName,
    @JsonProperty("middle_name") String middleName,
    String nickname,
    @JsonProperty("preferred_username") String preferredUsername,
    String profile,
    String picture,
    String website,
    String email,
    @JsonProperty("email_verified") Boolean emailVerified,
    String gender,
    String birthdate,
    String zoneinfo,
    String locale,
    @JsonProperty("phone_number") String phoneNumber,
    @JsonProperty("phone_number_verified") Boolean phoneNumberVerified,
    AddressInfo address,
    @JsonProperty("updated_at") Long updatedAt
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddressInfo(
        String formatted,
        @JsonProperty("street_address") String streetAddress,
        String locality,
        String region,
        @JsonProperty("postal_code") String postalCode,
        String country
    ) {}
}