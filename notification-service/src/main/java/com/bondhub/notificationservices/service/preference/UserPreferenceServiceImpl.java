package com.bondhub.notificationservices.service.preference;

import com.bondhub.common.dto.client.userservice.user.response.UserNotificationPreferenceResponse;
import com.bondhub.common.enums.NotificationType;
import com.bondhub.notificationservices.client.UserServiceClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPreferenceServiceImpl implements UserPreferenceService {

    UserServiceClient userServiceClient;

    @Override
    public boolean allow(String userId, NotificationType type) {
        try {
            var response = userServiceClient.getNotificationPreferences(userId);
            if (response.getBody() != null && response.getBody().data() != null) {
                return response.getBody().data().isAllowNotifications();
            }
        } catch (Exception e) {
            return true; 
        }
        return true;
    }

    @Override
    public String getLocale(String userId) {
        try {
            var response = userServiceClient.getNotificationPreferences(userId);
            if (response.getBody() != null && response.getBody().data() != null) {
                return response.getBody().data().getLanguage();
            }
        } catch (Exception e) {
            return "vi"; 
        }
        return "vi";
    }

    @Override
    public UserNotificationPreferenceResponse getInternalPreferences(String userId) {
        try {
            var response = userServiceClient.getNotificationPreferences(userId);
            if (response.getBody() != null && response.getBody().data() != null) {
                return response.getBody().data();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
