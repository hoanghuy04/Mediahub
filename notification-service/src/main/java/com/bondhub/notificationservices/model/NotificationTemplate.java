package com.bondhub.notificationservices.model;

import com.bondhub.common.model.BaseModel;
import com.bondhub.notificationservices.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("notification_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationTemplate extends BaseModel {
    @MongoId(FieldType.OBJECT_ID)
    String id;

    @Indexed
    NotificationType type;

    @Indexed
    private String locale;

    private String titleTemplate;

    private String bodyTemplate;

    String language;
}
