package com.bondhub.authservice.dto.auth.request;

import com.bondhub.authservice.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Login request DTO
 */
public record LoginRequest(
        @NotBlank(message = "Email is required") @jakarta.validation.constraints.Email(message = "Invalid email format") String email,

        @NotBlank(message = "Password is required") String password,

        @NotBlank(message = "Device ID is required") String deviceId,

        @NotNull(message = "Device type is required") DeviceType deviceType) {
}
