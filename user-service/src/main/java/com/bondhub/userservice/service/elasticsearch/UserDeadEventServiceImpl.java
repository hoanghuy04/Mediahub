package com.bondhub.userservice.service.elasticsearch;

import com.bondhub.common.utils.DateUtil;

import com.bondhub.common.dto.OutboxResponse;
import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.common.model.kafka.EventType;
import com.bondhub.common.model.kafka.OutboxEvent;
import com.bondhub.common.publisher.OutboxEventPublisher;
import com.bondhub.common.repository.OutboxEventRepository;
import com.bondhub.userservice.dto.request.elasticsearch.DeadEventRetryRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bondhub.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeadEventServiceImpl implements UserDeadEventService {

    OutboxEventRepository outboxEventRepository;
    OutboxEventPublisher outboxEventPublisher;
    ReindexTaskTracker reindexTaskTracker;
    MongoTemplate mongoTemplate;

    @Override
    public int retryDeadEvents(DeadEventRetryRequest request) {
        if (reindexTaskTracker.isReindexRunning()) {
            throw new AppException(ErrorCode.EL_REINDEX_IN_PROGRESS);
        }

        LocalDateTime fromDate = DateUtil.toLocalDateTime(request.fromDate());
        LocalDateTime toDate = DateUtil.toLocalDateTime(request.toDate());

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(OutboxEvent.OutboxEventStatus.DEAD));
        query.addCriteria(Criteria.where("eventType").in(List.of(EventType.USER_INDEX_REQUESTED, EventType.USER_INDEX_DELETED)));

        if (fromDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(fromDate));
        }
        if (toDate != null) {
            query.addCriteria(Criteria.where("createdAt").lte(toDate));
        }

        query.limit(1000);

        List<OutboxEvent> deadEvents = mongoTemplate.find(query, OutboxEvent.class);

        if (deadEvents.isEmpty()) {
            log.info("No DEAD user index events found to retry.");
            return 0;
        }

        log.info("Manual Retry: Found {} DEAD user index events to retry", deadEvents.size());
        return processRetryBatch(deadEvents);
    }

    @Scheduled(fixedDelayString = "${outbox.user-index.retry-interval:1800000}")
    public void autoRetryUserIndexEvents() {
        if (reindexTaskTracker.isReindexRunning()) {
            log.info("Auto Retry skipped: reindex is currently running.");
            return;
        }

        List<OutboxEvent> deadEvents = outboxEventRepository.findByEventTypeInAndStatusAndUpdatedAtBefore(
                List.of(EventType.USER_INDEX_REQUESTED, EventType.USER_INDEX_DELETED),
                OutboxEvent.OutboxEventStatus.DEAD,
                Instant.now().minusSeconds(3600)
        );

        if (deadEvents.isEmpty()) return;

        log.info("Auto Retry: Found {} old DEAD user index events to resurrect", deadEvents.size());
        processRetryBatch(deadEvents);
    }

    @Override
    public long countDeadEvents() {
        try {
            return outboxEventRepository.countByEventTypeInAndStatus(
                    List.of(EventType.USER_INDEX_REQUESTED, EventType.USER_INDEX_DELETED),
                    OutboxEvent.OutboxEventStatus.DEAD
            );
        } catch (Exception e) {
            log.error("Failed to count dead events: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public List<OutboxEvent> getDeadEvents() {
        try {
            return outboxEventRepository.findByEventTypeInAndStatus(
                    List.of(EventType.USER_INDEX_REQUESTED, EventType.USER_INDEX_DELETED),
                    OutboxEvent.OutboxEventStatus.DEAD
            );
        } catch (Exception e) {
            log.error("Failed to fetch dead events: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public PageResponse<List<OutboxResponse>> getDeadEventsPaged(
            String search,
            EventType eventType,
            ZonedDateTime fromDate,
            ZonedDateTime toDate,
            Pageable pageable
    ) {
        LocalDateTime localFromDate = DateUtil.toLocalDateTime(fromDate);
        LocalDateTime localToDate = DateUtil.toLocalDateTime(toDate);

        Query query = new Query();

        query.addCriteria(Criteria.where("status").is(OutboxEvent.OutboxEventStatus.DEAD));
        query.addCriteria(Criteria.where("eventType").in(List.of(EventType.USER_INDEX_REQUESTED, EventType.USER_INDEX_DELETED)));

        if (StringUtils.hasText(search)) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("aggregateId").regex(search, "i"),
                    Criteria.where("errorMessage").regex(search, "i")
            ));
        }

        if (eventType != null) {
            query.addCriteria(Criteria.where("eventType").is(eventType));
        }



        if (localFromDate != null) {
            query.addCriteria(Criteria.where("createdAt").gte(localFromDate));
        }
        if (localToDate != null) {
            query.addCriteria(Criteria.where("createdAt").lte(localToDate));
        }

        long total = mongoTemplate.count(query, OutboxEvent.class);

        query.with(pageable);

        List<OutboxEvent> events = mongoTemplate.find(query, OutboxEvent.class);
        Page<OutboxEvent> eventPage = new PageImpl<>(events, pageable, total);

        return PageResponse.fromPage(eventPage, event -> OutboxResponse.builder()
                .id(event.getId())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .retryCount(event.getRetryCount())
                .errorMessage(event.getErrorMessage())
                .payload(event.getPayload())
                .createdAt(event.getCreatedAt())
                .lastModifiedAt(event.getLastModifiedAt())
                .build());
    }

    private int processRetryBatch(List<OutboxEvent> events) {
        int successCount = 0;
        for (OutboxEvent event : events) {
            try {
                event.setStatus(OutboxEvent.OutboxEventStatus.PENDING);
                event.setRetryCount(0);
                event.setErrorMessage(null);
                outboxEventRepository.save(event);
                
                outboxEventPublisher.publishToKafka(event);
                successCount++;
            } catch (Exception e) {
                log.error("Retry Failed for event {}: {}", event.getId(), e.getMessage());
            }
        }
        return successCount;
    }
}
