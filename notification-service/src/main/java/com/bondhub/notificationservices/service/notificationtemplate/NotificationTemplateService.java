package com.bondhub.notificationservices.service.notificationtemplate;

import com.bondhub.notificationservices.dto.request.notificationtemplate.CreateTemplateRequest;
import com.bondhub.notificationservices.dto.request.notificationtemplate.UpdateTemplateRequest;
import com.bondhub.notificationservices.dto.response.notificationtemplate.NotificationTemplateResponse;
import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.enums.NotificationType;

import java.util.Map;

public interface NotificationTemplateService {

    NotificationTemplateResponse create(CreateTemplateRequest request);

    NotificationTemplateResponse update(String id, UpdateTemplateRequest request);

    NotificationTemplateResponse getTemplate(NotificationType type, NotificationChannel channel, String locale);

    String renderTitle(NotificationType type, NotificationChannel channel, String locale, Map<String, Object> data);

    String renderBody(NotificationType type, NotificationChannel channel, String locale, Map<String, Object> data);
}
