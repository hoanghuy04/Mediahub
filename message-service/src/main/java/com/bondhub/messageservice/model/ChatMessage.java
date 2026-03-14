package com.bondhub.messageservice.model;

import com.bondhub.common.model.BaseModel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "chatId_createdAt_idx", def = "{'chatId': 1, 'createdAt': -1}")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseModel {
    @Id
    String id;
    String chatId;
    String senderId;
    String senderName;
    String senderAvatar;
    String recipientId;
    String content;
    public enum MessageType {
        CHAT, JOIN, LEAVE, IMAGE, FILE
    }
    MessageType type;
}
