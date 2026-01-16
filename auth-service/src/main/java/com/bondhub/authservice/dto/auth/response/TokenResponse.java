package com.bondhub.authservice.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token response DTO containing JWT tokens
 */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,

        @JsonProperty("refresh_token") String refreshToken,

        @JsonProperty("token_type") String tokenType,

        @JsonProperty("expires_in") Long expiresIn) {
    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
