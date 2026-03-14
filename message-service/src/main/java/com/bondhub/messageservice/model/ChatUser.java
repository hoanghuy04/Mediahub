package com.bondhub.messageservice.model;

import com.bondhub.common.enums.Status;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatUser {
    @Id
    String id; // Same as UserProfile.id
    String accountId;
    String fullName;
    String email;
    Status status;
    String avatar;
    Instant lastUpdatedAt;
}
