package com.bondhub.notificationservices.service.notification;

import com.bondhub.notificationservices.dto.request.notification.CreateFriendRequestNotificationRequest;
import com.bondhub.notificationservices.dto.response.notification.NotificationAcceptedResponse;
import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.event.RawNotificationEvent;
import com.bondhub.notificationservices.publisher.RawNotificationPublisher;
import com.bondhub.notificationservices.service.notification.assembler.NotificationAssemblerResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationAssemblerResolver assemblerResolver;
    RawNotificationPublisher rawPublisher;

    @Override
    public NotificationAcceptedResponse createFriendRequestNotification(
            CreateFriendRequestNotificationRequest request) {
        return enqueue(NotificationType.FRIEND_REQUEST, request);
    }

    private NotificationAcceptedResponse enqueue(NotificationType type, Object request) {
        RawNotificationEvent event = assemblerResolver.get(type).build(request);
        log.info("[API] Enqueueing notification: type={}, recipient={}", type, event.getRecipientId());
        rawPublisher.publish(event);
        return NotificationAcceptedResponse.queued();
    }
}
