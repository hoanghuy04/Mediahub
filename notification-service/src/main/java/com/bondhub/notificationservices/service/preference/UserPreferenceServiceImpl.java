package com.bondhub.notificationservices.service.preference;

import com.bondhub.notificationservices.enums.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    @Override
    public boolean allow(String userId, NotificationType type) {
        return true;
    }

    @Override
    public String getLocale(String userId) {
        return "vi";
    }
}
