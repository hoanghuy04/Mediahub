package com.bondhub.userservice.service.elasticsearch;

import com.bondhub.common.dto.OutboxResponse;
import com.bondhub.common.model.kafka.OutboxEvent;
import com.bondhub.common.model.kafka.EventType;
import com.bondhub.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import java.time.ZonedDateTime;
import java.util.List;

import com.bondhub.userservice.dto.request.elasticsearch.DeadEventRetryRequest;

public interface UserDeadEventService {
    int retryDeadEvents(DeadEventRetryRequest request);
    long countDeadEvents();
    List<OutboxEvent> getDeadEvents();
    PageResponse<List<OutboxResponse>> getDeadEventsPaged(
            String search,
            EventType eventType,
            ZonedDateTime fromDate,
            ZonedDateTime toDate,
            Pageable pageable
    );
}
