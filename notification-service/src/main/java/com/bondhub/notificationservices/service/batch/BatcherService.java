package com.bondhub.notificationservices.service.batch;

import com.bondhub.notificationservices.event.RawNotificationEvent;

public interface BatcherService {
    boolean buffer(RawNotificationEvent event);
}
