package com.bondhub.messageservice.service;

import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.messageservice.dto.response.ConversationResponse;
import com.bondhub.messageservice.event.UserSyncEvent;
import com.bondhub.messageservice.model.ChatRoom;
import com.bondhub.messageservice.model.ChatUser;
import com.bondhub.messageservice.repository.ChatRoomRepository;
import com.bondhub.messageservice.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
        private final ChatRoomRepository chatRoomRepository;
        private final ChatUserRepository chatUserRepository;
        private final SecurityUtil securityUtil;
        private final ApplicationEventPublisher eventPublisher;

        public String generateChatRoomId(String senderId, String recipientId) {
                return (senderId.compareTo(recipientId) < 0)
                                ? String.format("%s_%s", senderId, recipientId)
                                : String.format("%s_%s", recipientId, senderId);
        }

        public Optional<String> getChatRoomId(
                        String senderId,
                        String recipientId,
                        boolean createNewRoomIfNotExists) {
                String chatId = generateChatRoomId(senderId, recipientId);

                return chatRoomRepository
                                .findByChatId(chatId)
                                .map(ChatRoom::getChatId)
                                .or(() -> {
                                        if (createNewRoomIfNotExists) {
                                                ChatRoom chatRoom = ChatRoom
                                                                .builder()
                                                                .chatId(chatId)
                                                                .senderId(senderId)
                                                                .recipientId(recipientId)
                                                                .build();

                                                chatRoomRepository.save(chatRoom);

                                                return Optional.of(chatId);
                                        }

                                        return Optional.empty();
                                });
        }

        public ChatRoom createInitialChatRoom(String userA, String userB, LocalDateTime timestamp) {
                String chatId = generateChatRoomId(userA, userB);

                return chatRoomRepository.findByChatId(chatId).orElseGet(() -> {
                        ChatRoom newRoom = ChatRoom.builder()
                                        .chatId(chatId)
                                        .senderId(userA)
                                        .recipientId(userB)
                                        .lastMessage(null) // Empty message to trigger UI greeting
                                        .lastMessageTime(timestamp) // Allows sorting in inbox
                                        .build();
                        log.info("Created initial chat room proactively for: {}", chatId);
                        return chatRoomRepository.save(newRoom);
                });
        }

        public ConversationResponse getConversationForUser(String userId, String partnerId) {
                String chatId = generateChatRoomId(userId, partnerId);
                ChatRoom room = chatRoomRepository.findByChatId(chatId)
                        .orElseThrow(() -> new RuntimeException("Room not found"));
                
                ChatUser partner = chatUserRepository.findById(partnerId)
                                .orElseGet(() -> ChatUser.builder().id(partnerId).fullName("Người dùng mới").build());

                return ConversationResponse.builder()
                                .chatId(room.getChatId())
                                .partnerId(partnerId)
                                .partnerName(partner.getFullName())
                                .partnerAvatar(partner.getAvatar())
                                .partnerStatus(partner.getStatus())
                                .lastSeenAt(partner.getLastUpdatedAt())
                                .lastMessage(room.getLastMessage())
                                .lastMessageTime(room.getLastMessageTime())
                                .build();
        }

        public List<ConversationResponse> getUserConversations() {
                String currentUserId = securityUtil.getCurrentUserId();
                List<ChatRoom> rooms = chatRoomRepository.findAllRoomsByUserId(currentUserId);
                if (rooms.isEmpty())
                        return List.of();

                // 1. Lấy tất cả partnerId
                Set<String> allPartnerIds = rooms.stream()
                                .map(room -> room.getSenderId().equals(currentUserId) ? room.getRecipientId()
                                                : room.getSenderId())
                                .collect(Collectors.toSet());

                // 2. Query batch từ Mirror DB
                List<ChatUser> partners = chatUserRepository.findAllById(allPartnerIds);
                Map<String, ChatUser> partnerMap = partners.stream()
                                .collect(Collectors.toMap(ChatUser::getId, u -> u));

                // 3. Tìm những ID bị thiếu để bắn Event (Chỉ bắn 1 lần cho mỗi ID)
                allPartnerIds.stream()
                                .filter(id -> !partnerMap.containsKey(id))
                                .forEach(id -> eventPublisher.publishEvent(new UserSyncEvent(id)));

                // 4. Map sang Response
                return rooms.stream().map(room -> {
                        String partnerId = room.getSenderId().equals(currentUserId) ? room.getRecipientId()
                                        : room.getSenderId();
                        ChatUser partner = partnerMap.getOrDefault(partnerId,
                                        ChatUser.builder().id(partnerId).fullName("Người dùng mới").build());

                        return ConversationResponse.builder()
                                        .chatId(room.getChatId())
                                        .partnerId(partnerId)
                                        .partnerName(partner.getFullName())
                                        .partnerAvatar(partner.getAvatar())
                                        .partnerStatus(partner.getStatus())
                                        .lastSeenAt(partner.getLastUpdatedAt())
                                        .lastMessage(room.getLastMessage())
                                        .lastMessageTime(room.getLastMessageTime())
                                        .build();
                }).toList();
        }
}
