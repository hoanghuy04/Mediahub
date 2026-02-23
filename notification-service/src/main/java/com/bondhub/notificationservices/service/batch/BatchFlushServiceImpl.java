package com.bondhub.notificationservices.service.batch;

import com.bondhub.notificationservices.enums.BatchStatus;
import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.event.BatchedNotificationEvent;
import com.bondhub.notificationservices.event.RawNotificationEvent;
import com.bondhub.notificationservices.model.NotificationBatch;
import com.bondhub.notificationservices.repository.NotificationBatchRepository;
import com.bondhub.notificationservices.service.notificationtemplate.NotificationTemplateService;
import com.bondhub.notificationservices.service.preference.UserPreferenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BatchFlushServiceImpl implements BatchFlushService {

    static final String LOCK_PREFIX = "batch:lock:";
    static final String LIST_PREFIX = "batch:";

    StringRedisTemplate stringRedisTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;
    NotificationBatchRepository notificationBatchRepository;
    BatchScheduler batchScheduler;
    NotificationTemplateService templateService;
    UserPreferenceService userPreferenceService;

    @Value("${app.kafka.topics.delivery-queue}")
    @NonFinal
    String deliveryTopic;

    @Value("${app.batch.recovery-on-startup}")
    @NonFinal
    boolean recoveryOnStartup;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @PostConstruct
    void recoverOpenBatches() {
        if (!recoveryOnStartup)  return;

        List<NotificationBatch> stale = notificationBatchRepository
                .findByStatusAndWindowExpiresAtBefore(BatchStatus.OPEN, LocalDateTime.now());

        if(!stale.isEmpty()) {
            log.info("Recovering {} stale OPEN batches after restart", stale.size());
            stale.forEach(b -> batchScheduler.scheduleFlush(b.getBatchKey(), 0));
        }
    }

    @Override
    public void flush(String batchKey) {
        String listKey =  LIST_PREFIX + batchKey;
        String lockKey = LOCK_PREFIX + batchKey;

        List<String> rawList = stringRedisTemplate.opsForList().range(listKey, 0, -1);
        stringRedisTemplate.delete(listKey);
        stringRedisTemplate.delete(lockKey);

        if(rawList == null  || rawList.isEmpty()) {
            log.debug("Flush no-op: empty list for batchKey={}", batchKey);
            return;
        }

        List<RawNotificationEvent> events = rawList.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();

        if(events.isEmpty()) return;

        // Aggregate
        RawNotificationEvent first = events.getFirst();
        List<String> actorIds = events.stream()
                .map(RawNotificationEvent::getActorId)
                .distinct()
                .toList();

        int actorCount = actorIds.size();
        int othersCount = actorCount - 1;
        String firstActorId     = first.getActorId();
        String firstActorName   = first.getActorName() != null ? first.getActorName() : firstActorId;
        String firstActorAvatar = first.getActorAvatar();
        // TODO: Call feign client to user-service to get locale
        String locale           = userPreferenceService.getLocale(first.getRecipientId());

        // TemplateEngine
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("firstName",   firstActorName);
        templateData.put("othersCount", othersCount);
        templateData.put("count",       actorCount);

        String renderedTitle = renderSafe(first.getType(), NotificationChannel.IN_APP, locale, templateData, "title");
        String renderedBody  = renderSafe(first.getType(), NotificationChannel.IN_APP, locale, templateData, "body");

        // Raw payloads
        List<Map<String, Object>> rawPayloads = events.stream()
                .map(e -> {
                    Map<String, Object> p = new HashMap<>(
                            e.getPayload() != null ? e.getPayload() : Collections.emptyMap()
                    );
                    p.put("actorId",     e.getActorId());
                    p.put("referenceId", e.getReferenceId());
                    p.put("occurredAt",  e.getOccurredAt() != null ? e.getOccurredAt().toString() : null);
                    return p;
                }).toList();

        BatchedNotificationEvent batched = BatchedNotificationEvent.builder()
                .recipientId(first.getRecipientId())
                .type(first.getType())
                .actorIds(actorIds)
                .actorCount(actorCount)
                .firstActorId(firstActorId)
                .firstActorName(firstActorName)
                .firstActorAvatar(firstActorAvatar)
                .othersCount(othersCount)
                .locale(locale)
                .renderedTitle(renderedTitle)
                .renderedBody(renderedBody)
                .rawPayloads(rawPayloads)
                .batchedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(deliveryTopic, first.getRecipientId(), batched);
        log.info("Batch flushed: key={}, actors={}, topic={}", batchKey, actorCount, deliveryTopic);

        notificationBatchRepository.findByBatchKey(batchKey).ifPresent(b -> {
            b.setStatus(BatchStatus.FLUSHED);
            notificationBatchRepository.save(b);
        });

    }

    private String renderSafe(NotificationType type, NotificationChannel channel,
                              String locale, Map<String, Object> data, String field) {
        try {
            return "title".equals(field)
                    ? templateService.renderTitle(type, channel, locale, data)
                    : templateService.renderBody(type, channel, locale, data);
        } catch (Exception e){
            log.warn("Template not found for type={} channel={} locale={}, using fallback", type, channel, locale);
            return "title".equals(field)
                    ? data.get("count") + " new notifications"
                    : "";
        }
    }

    private RawNotificationEvent deserialize(String json) {
        try {
            return objectMapper.readValue(json, RawNotificationEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Deserialize failed: {}", json, e);
            return null;
        }
    }
}
