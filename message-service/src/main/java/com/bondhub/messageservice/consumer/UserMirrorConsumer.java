package com.bondhub.messageservice.consumer;

import com.bondhub.common.event.user.UserProfileUpdatedEvent;
import com.bondhub.messageservice.model.ChatUser;
import com.bondhub.messageservice.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserMirrorConsumer {

    private final ChatUserRepository chatUserRepository;

    @KafkaListener(topics = "${kafka.topics.user-events.updated}", groupId = "${spring.kafka.consumer.group-id:message-service-group}")
    public void handleUserUpdated(UserProfileUpdatedEvent event, Acknowledgment ack) {
        log.info("Received USER_UPDATED event for userId: {}", event.userId());
        try {
            chatUserRepository.findById(event.userId()).ifPresentOrElse(user -> {
                LocalDateTime eventTime = new Timestamp(event.timestamp()).toLocalDateTime();
                // Idempotency: Only update if the event is newer than the last update
                if (user.getLastUpdatedAt() == null || user.getLastUpdatedAt().isBefore(eventTime)) {
                    user.setFullName(event.fullName());
                    user.setAvatar(event.avatar());
                    user.setLastUpdatedAt(eventTime);
                    chatUserRepository.save(user);
                    log.info("✅ Updated ChatUser mirror for userId: {}", event.userId());
                } else {
                    log.info("⏩ Skipped outdated USER_UPDATED event for userId: {}", event.userId());
                }
            }, () -> {
                // Cold start/New user: Create mirror entry
                ChatUser newUser = ChatUser.builder()
                        .id(event.userId())
                        .fullName(event.fullName())
                        .avatar(event.avatar())
                        .lastUpdatedAt(new Timestamp(event.timestamp()).toLocalDateTime())
                        .build();
                chatUserRepository.save(newUser);
                log.info("✅ Created new ChatUser mirror for userId: {}", event.userId());
            });
            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ Error processing USER_UPDATED event for userId: {}", event.userId(), e);
            // In a real production scenario, we might want to retry or send to a DLT
        }
    }
}
