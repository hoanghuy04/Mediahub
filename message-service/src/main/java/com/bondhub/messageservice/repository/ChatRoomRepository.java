package com.bondhub.messageservice.repository;

import com.bondhub.messageservice.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findBySenderIdAndRecipientId(String senderId, String recipientId);

    Optional<ChatRoom> findByChatId(String chatId);

    @Query(value = "{ '$or': [ { 'senderId': ?0 }, { 'recipientId': ?0 } ] }", sort = "{ 'lastMessageTime' : -1 }")
    List<ChatRoom> findAllRoomsByUserId(String userId);
}
