package com.bondhub.authservice.dto.auth.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record QrGenerationResponse(
        String qrId,
        String qrContent,
        Instant expiresAt
) {}