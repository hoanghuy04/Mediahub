package com.bondhub.userservice.dto.response;

import com.bondhub.userservice.model.enums.Gender;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response containing detailed information about a blocked user including user profile
 */
@Builder
public record BlockedUserDetailResponse(
    String id,
    String blockedUserId,
    String fullName,
    String avatar,
    String bio,
    Gender gender,
    LocalDate dob,
    BlockPreferenceResponse preference,
    LocalDateTime blockedAt
) {}
