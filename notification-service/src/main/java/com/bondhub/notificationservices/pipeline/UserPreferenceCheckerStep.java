package com.bondhub.notificationservices.pipeline;

import com.bondhub.common.event.notification.RawNotificationEvent;
import com.bondhub.notificationservices.service.preference.UserPreferenceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPreferenceCheckerStep implements PipelineStep {

    UserPreferenceService userPreferenceService;

    @Override
    public boolean process(RawNotificationEvent event) {
        boolean isAllowed = userPreferenceService.allow(event.getRecipientId(), event.getType());
        if (!isAllowed) {
            log.info("[Pipeline] Notification skipped due to user preference: recipientId={}, type={}", 
                    event.getRecipientId(), event.getType());
        }
        return isAllowed;
    }
}
