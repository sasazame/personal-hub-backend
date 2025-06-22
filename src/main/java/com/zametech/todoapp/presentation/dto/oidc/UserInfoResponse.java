package com.zametech.todoapp.presentation.dto.oidc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponse(
    String sub,
    String name,
    String givenName,
    String familyName,
    String middleName,
    String nickname,
    String preferredUsername,
    String profile,
    String picture,
    String website,
    String email,
    Boolean emailVerified,
    String gender,
    String birthdate,
    String zoneinfo,
    String locale,
    String phoneNumber,
    Boolean phoneNumberVerified,
    AddressInfo address,
    Long updatedAt
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddressInfo(
        String formatted,
        String streetAddress,
        String locality,
        String region,
        String postalCode,
        String country
    ) {}
}