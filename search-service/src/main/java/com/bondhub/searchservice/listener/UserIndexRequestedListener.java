package com.bondhub.searchservice.listener;

import com.bondhub.common.event.user.UserIndexRequestedEvent;
import com.bondhub.searchservice.model.elasticsearch.UserIndex;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserIndexRequestedListener {
    ElasticsearchOperations esOperations;
    com.bondhub.searchservice.config.ElasticsearchProperties esProperties;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".dlq",
            include = {Exception.class}
    )
    @KafkaListener(
            topics = "#{kafkaTopicProperties.userEvents.indexRequested}",
            groupId = "search-service-indexer-group",
            concurrency = "3"
    )
    public void handleIndexRequest(
            @Payload UserIndexRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Processing index request: userId={}, partition={}, offset={}",
                event.userId(), partition, offset);

        try {
            UserIndex userIndex = convertToUserIndex(event);
            esOperations.save(userIndex, IndexCoordinates.of(esProperties.getUserAlias()));
            
            ack.acknowledge();
            log.info("Successfully indexed user: {}", event.userId());

        } catch (Exception e) {
            log.error("Failed to index user: userId={}", event.userId(), e);
            throw e;
        }
    }

    private UserIndex convertToUserIndex(UserIndexRequestedEvent event) {
        String roleName = "USER";
        if (event.role() != null) {
            try {
                roleName = event.role().getName();
            } catch (Exception e) {
                log.warn("Failed to get role name, defaulting to USER: {}", e.getMessage());
            }
        }

        return UserIndex.builder()
                .id(event.userId())
                .fullName(event.fullName())
                .phoneNumber(event.phoneNumber())
                .accountId(event.accountId())
                .role(roleName)
                .avatar(event.avatar())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
