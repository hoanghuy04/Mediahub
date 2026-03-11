package com.bondhub.searchservice.listener;

import com.bondhub.common.event.user.UserIndexDeletedEvent;
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

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserIndexDeletedListener {
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
            topics = "#{kafkaTopicProperties.userEvents.indexDeleted}",
            groupId = "search-service-indexer-group",
            concurrency = "3"
    )
    public void handleDeleteRequest(
            @Payload UserIndexDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Processing delete request: userId={}, partition={}, offset={}",
                event.userId(), partition, offset);

        try {
            esOperations.delete(event.userId(), IndexCoordinates.of(esProperties.getUserAlias()));
            ack.acknowledge();
            log.info("Successfully deleted user from index: {}", event.userId());

        } catch (Exception e) {
            log.error("Failed to delete user: userId={}", event.userId(), e);
            throw e;
        }
    }
}
