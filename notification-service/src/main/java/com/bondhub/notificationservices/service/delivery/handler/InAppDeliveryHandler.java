package com.bondhub.notificationservices.service.delivery.handler;

import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.event.BatchedNotificationEvent;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.repository.NotificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InAppDeliveryHandler {

    NotificationRepository notificationRepository;
    MongoTemplate mongoTemplate;

    public Notification persistAndReturn(BatchedNotificationEvent event) {
        return event.getReferenceId() != null
                ? persistPerEntity(event)
                : persistAggregate(event);
    }

    private Notification persistPerEntity(BatchedNotificationEvent event) {
        int actorCount = 1;
        int othersCount = 0;

        Notification notification = Notification.builder()
                .userId(event.getRecipientId())
                .type(event.getType())
                .referenceId(event.getReferenceId())
                .actorIds(new ArrayList<>(List.of(event.getLastActorId())))
                .data(buildData(event.getLastActorId(), event.getLastActorName(), event.getLastActorAvatar(), actorCount, othersCount))
                .isRead(false)
                .build();

        notification.setLastModifiedAt(event.getLastOccurredAt());
        notification.setCreatedAt(event.getLastOccurredAt());

        Notification saved = notificationRepository.save(notification);
        log.info("IN_APP per-entity persisted: recipientId={}, type={}, referenceId={}",
                event.getRecipientId(), event.getType(), event.getReferenceId());
        return saved;
    }

    private Notification persistAggregate(BatchedNotificationEvent event) {
        List<String> rawActorIds = event.getActorIds() != null ? event.getActorIds() : List.of();
        List<ObjectId> actorObjectIds = rawActorIds.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        Query query = new Query(Criteria.where("userId").is(event.getRecipientId())
                .and("type").is(event.getType())
                .and("referenceId").is(null));

        Update update = new Update()
                .push("actorIds").atPosition(Update.Position.LAST).each(actorObjectIds.toArray())
                .set("isRead", false)
                .set("lastModifiedAt", event.getLastOccurredAt());

        Notification persisted = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Notification.class
        );

        if (persisted == null) {
            log.warn("IN_APP aggregate upsert returned null: recipientId={}, type={}",
                    event.getRecipientId(), event.getType());
            return null;
        }

        Set<String> unique = new LinkedHashSet<>(persisted.getActorIds());
        List<String> finalActors = new ArrayList<>(unique);
        persisted.setActorIds(finalActors);

        int actorCount = finalActors.size();
        int othersCount = actorCount - 1;
        String lastActorId = finalActors.getLast();

        persisted.setData(buildData(lastActorId, event.getLastActorName(), event.getLastActorAvatar(), actorCount, othersCount));

        notificationRepository.save(persisted);
        log.info("IN_APP aggregate persisted: recipientId={}, type={}, totalActors={}",
                event.getRecipientId(), event.getType(), actorCount);
        return persisted;
    }

    private Map<String, Object> buildData(String actorId, String actorName, String actorAvatar, int actorCount, int othersCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("actorId", actorId);
        data.put("actorName", actorName);
        data.put("firstName", actorName != null ? actorName : actorId);
        data.put("actorAvatar", actorAvatar);
        data.put("actorCount", actorCount);
        data.put("othersCount", othersCount);
        data.put("count", actorCount);
        return data;
    }
}