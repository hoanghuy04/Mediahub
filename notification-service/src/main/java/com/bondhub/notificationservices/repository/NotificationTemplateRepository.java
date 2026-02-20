package com.bondhub.notificationservices.repository;

import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.model.NotificationTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByTypeAndLocaleAndActiveTrue(
            NotificationType type,
            String locale
    );
}