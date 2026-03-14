package com.bondhub.messageservice.service;

import com.bondhub.common.dto.client.userservice.user.response.UserSummaryResponse;
import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.messageservice.client.UserServiceClient;
import com.bondhub.messageservice.dto.response.ChatNotification;
import com.bondhub.messageservice.model.ChatMessage;
import com.bondhub.messageservice.model.ChatRoom;
import com.bondhub.messageservice.model.ChatUser;
import com.bondhub.messageservice.repository.ChatMessageRepository;
import com.bondhub.messageservice.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatUserRepository chatUserRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtil securityUtil;
    private final UserServiceClient userServiceClient;
    private final MongoTemplate mongoTemplate;

    public ChatMessage save(ChatMessage chatMessage) {
        var chatId = chatRoomService
                .getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow(); // create our own custom exception
        chatMessage.setChatId(chatId);

        // Lookup sender info for snapshot (Cold Start if missing)
        ChatUser sender = chatUserRepository.findById(chatMessage.getSenderId())
                .orElseGet(() -> fetchAndSaveUserFromUserService(chatMessage.getSenderId()));

        chatMessage.setSenderName(sender.getFullName());
        chatMessage.setSenderAvatar(sender.getAvatar());

        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    private ChatUser fetchAndSaveUserFromUserService(String userId) {
        log.info("Cold Start: Fetching user {} from user-service", userId);
        UserSummaryResponse userDto = userServiceClient.getUserById(userId).data();
        ChatUser mirrorUser = ChatUser.builder()
                .id(userDto.id())
                .fullName(userDto.fullName())
                .avatar(userDto.avatar())
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        return chatUserRepository.save(mirrorUser);
    }

    public List<ChatMessage> findChatMessages(String recipientId) {
        String currentUserId = securityUtil.getCurrentUserId();
        var chatId = chatRoomService.getChatRoomId(currentUserId, recipientId, false);
        return chatId.map(chatMessageRepository::findByChatId).orElse(List.of());
    }

    public void sendMessage(ChatMessage chatMessage) {
        ChatMessage savedMsg = save(chatMessage);

        String previewContent = switch (savedMsg.getType() == null ? ChatMessage.MessageType.CHAT
                : savedMsg.getType()) {
            case IMAGE -> "[Hình ảnh]";
            case FILE -> "[Tệp tin]";
            default -> savedMsg.getContent();
        };

        Query query = new Query(Criteria.where("chatId").is(savedMsg.getChatId()));
        Update update = new Update()
                .set("lastMessage", previewContent)
                .set("lastMessageTime", savedMsg.getCreatedAt());
        mongoTemplate.updateFirst(query, update, ChatRoom.class);

        log.info("[Chat] Sending real-time message to: {}", savedMsg.getRecipientId());

        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipientId(),
                "/queue/messages",
                ChatNotification.builder()
                        .id(savedMsg.getId())
                        .chatId(savedMsg.getChatId())
                        .senderId(savedMsg.getSenderId())
                        .senderName(savedMsg.getSenderName())
                        .senderAvatar(savedMsg.getSenderAvatar())
                        .recipientId(savedMsg.getRecipientId())
                        .content(savedMsg.getContent())
                        .timestamp(savedMsg.getCreatedAt())
                        .build());
    }
}
