package com.bondhub.notificationservices.service.preference;

import com.bondhub.common.dto.client.userservice.user.response.UserNotificationPreferenceResponse;
import com.bondhub.common.enums.NotificationType;

public interface UserPreferenceService {

    boolean allow(String userId, NotificationType type);

    String getLocale(String userId);

    UserNotificationPreferenceResponse getInternalPreferences(String userId);
}
