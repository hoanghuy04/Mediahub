package com.bondhub.notificationservices.event;

import com.bondhub.notificationservices.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RawNotificationEvent {

    String recipientId;
    String actorId;
    String actorName;
    String actorAvatar;
    NotificationType type;
    String referenceId;

    Map<String, Object> payload;

    LocalDateTime occurredAt;
}