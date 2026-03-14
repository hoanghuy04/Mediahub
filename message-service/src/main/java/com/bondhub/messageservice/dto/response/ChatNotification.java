package com.bondhub.messageservice.dto.response;

import lombok.Builder;

/**
 * Chat Notification DTO
 * Uses Java Record as per backend development rules
 */
@Builder
public record ChatNotification(
    String id,
    String senderId,
    String senderName,
    String senderAvatar,
    String recipientId,
    String content
) {}
