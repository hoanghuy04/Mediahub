package com.bondhub.notificationservices.mapper;

import com.bondhub.notificationservices.dto.response.notification.NotificationResponse;
import com.bondhub.notificationservices.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "actorCount", expression = "java(notification.getActorIds() != null ? notification.getActorIds().size() : 0)")
    NotificationResponse toResponse(Notification notification, String title, String body);
}
