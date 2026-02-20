package com.bondhub.notificationservices.orchestrator;

import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.repository.NotificationRepository;
import com.bondhub.notificationservices.strategy.content.NotificationContentStrategy;
import com.bondhub.notificationservices.strategy.content.factory.ContentStrategyFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationOrchestrator {

    ContentStrategyFactory contentStrategyFactory;
    NotificationRepository notificationRepository;

    public Notification process(NotificationType type, Object request) {
        log.info("Orchestrator processing notification type={}", type);
        NotificationContentStrategy strategy = contentStrategyFactory.get(type);
        Notification notification = strategy.build(request);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved id={}", saved.getId());
        return saved;
    }
}
