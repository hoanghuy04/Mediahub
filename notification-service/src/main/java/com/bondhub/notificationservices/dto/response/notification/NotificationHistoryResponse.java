package com.bondhub.notificationservices.dto.response.notification;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationHistoryResponse(
        List<NotificationResponse> newest,
        List<NotificationResponse> today,
        List<NotificationResponse> previous,
        LocalDateTime nextCursor
) {
}
