package com.bondhub.authservice.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for verifying OTP and completing registration (Step 2)
 */
public record RegisterVerifyRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
        String otp
) {
}
