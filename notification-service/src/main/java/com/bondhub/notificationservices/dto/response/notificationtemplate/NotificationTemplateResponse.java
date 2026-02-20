package com.bondhub.notificationservices.dto.response.notificationtemplate;

import com.bondhub.notificationservices.enums.NotificationType;

public record NotificationTemplateResponse(
        String id,
        NotificationType type,
        String locale,
        String titleTemplate,
        String bodyTemplate,
        boolean active
) {}
