package com.bondhub.common.dto.client.userservice.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreferenceResponse {
    private String userId;
    private boolean allowNotifications;
    private String language;
    private Map<String, String> languageByDeviceId;
}
