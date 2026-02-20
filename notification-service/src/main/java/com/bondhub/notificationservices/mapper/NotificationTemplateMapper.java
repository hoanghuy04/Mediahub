package com.bondhub.notificationservices.mapper;

import com.bondhub.notificationservices.dto.request.notificationtemplate.CreateTemplateRequest;
import com.bondhub.notificationservices.dto.response.notificationtemplate.NotificationTemplateResponse;
import com.bondhub.notificationservices.model.NotificationTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationTemplateMapper {

    NotificationTemplate toEntity(CreateTemplateRequest request);

    NotificationTemplateResponse toResponse(NotificationTemplate entity);
}