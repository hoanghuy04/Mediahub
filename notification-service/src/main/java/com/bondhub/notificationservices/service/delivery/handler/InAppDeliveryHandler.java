package com.bondhub.notificationservices.service.delivery.handler;

import com.bondhub.notificationservices.enums.BatchWindowConfig;
import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.event.BatchedNotificationEvent;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.model.UserNotificationState;
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

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InAppDeliveryHandler {

    NotificationRepository notificationRepository;
    MongoTemplate mongoTemplate;

    public Notification persistAndReturn(BatchedNotificationEvent event) {
        BatchWindowConfig cfg =
            BatchWindowConfig.of(event.getType());

        if (!cfg.isIncludeReferenceInKey()) {
            return persistAggregate(event);
        }

        return event.getReferenceId() != null
                ? persistPerEntity(event)
                : persistAggregate(event);
    }

    private Notification persistPerEntity(BatchedNotificationEvent event) {
        List<String> rawActorIds = event.getActorIds() != null ? event.getActorIds() : List.of();
        List<ObjectId> actorObjectIds = rawActorIds.stream()
                .filter(ObjectId::isValid)
                .map(ObjectId::new)
                .toList();

        Query query = new Query(Criteria.where("userId").is(event.getRecipientId())
                .and("type").is(event.getType())
                .and("referenceId").is(event.getReferenceId()));

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

        if (persisted == null) return null;

        return finalizeAndSave(persisted, event);
    }

    private Notification finalizeAndSave(Notification persisted, BatchedNotificationEvent event) {
        Set<String> unique = new LinkedHashSet<>(persisted.getActorIds());
        List<String> finalActors = new ArrayList<>(unique);
        persisted.setActorIds(finalActors);

        int actorCount = finalActors.size();
        int othersCount = Math.max(0, actorCount - 1);

        List<Map<String, Object>> payloads = event.getRawPayloads() != null ? event.getRawPayloads() : List.of();
        Map<String, Object> basePayload = !payloads.isEmpty() ? payloads.get(payloads.size() - 1) : Map.of();
        Map<String, Object> payloadMap = new HashMap<>(basePayload);

        payloadMap.remove("firstName");
        payloadMap.remove("lastName");
        payloadMap.remove("count");
        payloadMap.remove("actorId");

        payloadMap.put("actorName", event.getLastActorName());
        payloadMap.put("actorAvatar", event.getLastActorAvatar());
        payloadMap.put("actorCount", actorCount);
        payloadMap.put("othersCount", othersCount);
        payloadMap.put("showSecondActor", actorCount == 2);

        if (actorCount == 2) {
            try {
                String secondToLastId = finalActors.get(finalActors.size() - 2);
                String secondName = payloads.stream()
                        .filter(p -> secondToLastId.equals(p.get("actorId")))
                        .map(p -> (String) p.get("actorName"))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("một người khác");
                payloadMap.put("secondActorName", secondName);
            } catch (Exception e) {
                payloadMap.put("secondActorName", "một người khác");
            }
        } else {
            payloadMap.remove("secondActorName");
        }

        persisted.setPayload(payloadMap);
        Notification saved = notificationRepository.save(persisted);

        incrementUnreadCount(event.getRecipientId());
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

        return finalizeAndSave(persisted, event);
    }

    private void incrementUnreadCount(String userId) {
        mongoTemplate.upsert(
                new Query(Criteria.where("userId").is(userId)),
                new Update().inc("unreadCount", 1L),
                UserNotificationState.class
        );
    }
}