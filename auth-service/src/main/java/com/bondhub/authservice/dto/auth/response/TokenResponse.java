package com.bondhub.authservice.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token response DTO containing JWT tokens
 */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("refresh_token") String refreshToken) {

    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}
