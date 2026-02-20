package com.bondhub.notificationservices.service.notification;

import com.bondhub.notificationservices.dto.request.notification.CreateFriendRequestNotificationRequest;
import com.bondhub.notificationservices.dto.response.notification.NotificationResponse;
import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.mapper.NotificationMapper;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.orchestrator.NotificationOrchestrator;
import com.bondhub.notificationservices.repository.NotificationRepository;
import com.bondhub.notificationservices.strategy.content.NotificationContentStrategy;
import com.bondhub.notificationservices.strategy.content.factory.ContentStrategyFactory;
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

    NotificationOrchestrator orchestrator;
    NotificationMapper mapper;

    @Override
    public NotificationResponse createFriendRequestNotification(CreateFriendRequestNotificationRequest request) {
        log.info("Creating friend request notification");
        Notification saved = orchestrator.process( NotificationType.FRIEND_REQUEST, request);
        return mapper.toResponse(saved);
    }
}
