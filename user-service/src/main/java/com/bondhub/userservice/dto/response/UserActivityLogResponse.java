package com.bondhub.userservice.dto.response;

import com.bondhub.userservice.model.enums.UserAction;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response containing user activity log information
 */
@Builder
public record UserActivityLogResponse(
    String id,
    String userId,
    UserAction action,
    String description,
    String ipAddress,
    String userAgent,
    Map<String, Object> metadata,
    LocalDateTime createdAt,
    String createdBy
) {}
