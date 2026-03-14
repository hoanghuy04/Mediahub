package com.bondhub.messageservice.service;

import com.bondhub.messageservice.model.ChatRoom;
import com.bondhub.messageservice.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public Optional<String> getChatRoomId(
            String senderId,
            String recipientId,
            boolean createNewRoomIfNotExists
    ) {
        String chatId = (senderId.compareTo(recipientId) < 0) 
                ? String.format("%s_%s", senderId, recipientId) 
                : String.format("%s_%s", recipientId, senderId);

        return chatRoomRepository
                .findByChatId(chatId) // Change to find by unique chatId
                .map(ChatRoom::getChatId)
                .or(() -> {
                    if (createNewRoomIfNotExists) {
                        ChatRoom senderRecipient = ChatRoom
                                .builder()
                                .chatId(chatId)
                                .senderId(senderId)
                                .recipientId(recipientId)
                                .build();

                        ChatRoom recipientSender = ChatRoom
                                .builder()
                                .chatId(chatId)
                                .senderId(recipientId)
                                .recipientId(senderId)
                                .build();

                        chatRoomRepository.save(senderRecipient);
                        chatRoomRepository.save(recipientSender);

                        return Optional.of(chatId);
                    }

                    return Optional.empty();
                });
    }
}
