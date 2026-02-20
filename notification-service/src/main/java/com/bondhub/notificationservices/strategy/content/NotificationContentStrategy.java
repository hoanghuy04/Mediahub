package com.bondhub.notificationservices.strategy.content;

import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.model.Notification;

public interface NotificationContentStrategy {
    Notification buildNotification(Object metadata, String receiverId, String language);
    NotificationType getSupportedType();
}
