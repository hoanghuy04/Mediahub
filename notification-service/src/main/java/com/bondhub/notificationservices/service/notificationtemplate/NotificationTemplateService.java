package com.bondhub.notificationservices.service.notificationtemplate;

import com.bondhub.notificationservices.dto.request.notificationtemplate.CreateTemplateRequest;
import com.bondhub.notificationservices.dto.request.notificationtemplate.UpdateTemplateRequest;
import com.bondhub.notificationservices.dto.response.notificationtemplate.NotificationTemplateResponse;
import com.bondhub.notificationservices.enums.NotificationType;

import java.util.Map;

public interface NotificationTemplateService {

    NotificationTemplateResponse create(CreateTemplateRequest request);

    NotificationTemplateResponse update(String id, UpdateTemplateRequest request);

    NotificationTemplateResponse getTemplate(NotificationType type, String locale);

    String renderTitle(NotificationType type, String locale, Map<String, Object> data);

    String renderBody(NotificationType type, String locale, Map<String, Object> data);
}
