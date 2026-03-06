package com.bondhub.notificationservices.service.delivery.handler;

import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.enums.Platform;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.model.UserDevice;
import com.bondhub.notificationservices.repository.UserDeviceRepository;
import com.bondhub.notificationservices.service.notificationtemplate.NotificationTemplateService;
import com.bondhub.notificationservices.service.preference.UserPreferenceService;
import com.bondhub.notificationservices.service.presence.PresenceService;
import com.google.firebase.messaging.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FcmDeliveryHandler {

    static final RetryTemplate FCM_RETRY = RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(500, 2, 4000)
            .retryOn(RuntimeException.class)
            .build();

    UserDeviceRepository userDeviceRepository;
    NotificationTemplateService templateService;
    PresenceService presenceService;
    UserPreferenceService userPreferenceService;

    public void push(Notification persisted) {
        String recipientId = persisted.getUserId();

        if (presenceService.isOnline(recipientId)) {
            log.debug("FCM skip: user is online, recipientId={}", recipientId);
            return;
        }

        List<UserDevice> devices = userDeviceRepository.findByUserId(recipientId);
        if (devices.isEmpty()) {
            log.debug("FCM skip: no devices for recipientId={}", recipientId);
            return;
        }

        String locale = userPreferenceService.getLocale(recipientId);

        int actorCount = persisted.getActorIds() != null ? persisted.getActorIds().size() : 0;
        int othersCount = Math.max(0, actorCount - 1);

        String lastActorId = getStr(persisted, "actorId");
        String lastActorName = getStr(persisted, "actorName");
        String lastActorAvatar = getStr(persisted, "actorAvatar");
        String requestId = getStr(persisted, "requestId");

        Map<String, Object> renderData = new HashMap<>(persisted.getPayload() != null ? persisted.getPayload() : Collections.emptyMap());
        renderData.put("actorCount", actorCount);
        renderData.put("othersCount", actorCount > 2 ? actorCount - 1 : 0);
        renderData.put("showSecondActor", actorCount == 2);
        renderData.put("actorName", lastActorName != null ? lastActorName : "");
        renderData.put("actorAvatar", lastActorAvatar != null ? lastActorAvatar : "");

        if (actorCount == 2) {
            String secondActorName = getStr(persisted, "secondActorName");
            renderData.put("secondActorName", secondActorName != null ? secondActorName : "một người khác");
        }

        var template = templateService.getTemplate(persisted.getType(), NotificationChannel.FCM, locale);
        String title = templateService.render(template.titleTemplate(), renderData);
        String body = templateService.render(template.bodyTemplate(), renderData);

        log.info("FCM processing: type={}, recipientId={}, locale={}, title='{}', body='{}'",
                persisted.getType(), recipientId, locale, title, body);

        if ("".equals(title) && "".equals(body)) {
            log.warn("FCM skip: both title and body are empty for type={}", persisted.getType());
            return;
        }

        String collapseKey = persisted.getType().name() + "_" + recipientId;

        for (UserDevice device : devices) {
            sendToDevice(device, title, body, collapseKey,
                    recipientId, lastActorId, lastActorName, lastActorAvatar,
                    actorCount, othersCount, persisted.getType().name(), requestId);
        }
    }

    private void sendToDevice(UserDevice device,
                              String title,
                              String body,
                              String collapseKey,
                              String recipientId,
                              String lastActorId,
                              String lastActorName,
                              String lastActorAvatar,
                              int actorCount,
                              int othersCount,
                              String type,
                              String requestId) {

        String categoryIdentifier = "FRIEND_REQUEST".equals(type) ? "friend_request" : "";

        Message.Builder messageBuilder = Message.builder()
                .setToken(device.getFcmToken())
                .putAllData(Map.of(
                        "type", type,
                        "title", title != null ? title : "",
                        "body", body != null ? body : "",
                        "actorId", lastActorId != null ? lastActorId : "",
                        "actorName", lastActorName != null ? lastActorName : "",
                        "actorAvatar", lastActorAvatar != null ? lastActorAvatar : "",
                        "actorCount", String.valueOf(actorCount),
                        "othersCount", String.valueOf(othersCount),
                        "categoryIdentifier", categoryIdentifier,
                        "requestId", requestId != null ? requestId : ""
                ));

        if (device.getPlatform() == Platform.WEB) {
            messageBuilder.setNotification(com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());

            messageBuilder.setWebpushConfig(WebpushConfig.builder()
                    .setNotification(WebpushNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setIcon(lastActorAvatar != null ? lastActorAvatar : "/images/logo.png")
                            .setTag(collapseKey)
                            .build())
                    .setFcmOptions(WebpushFcmOptions.withLink("http://localhost:5173/notifications"))
                    .build());
        }

        if (device.getPlatform() == Platform.ANDROID) {
            messageBuilder.setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setCollapseKey(collapseKey)
                    .build());
        }

        if (device.getPlatform() == Platform.IOS) {
            messageBuilder.setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setCategory(categoryIdentifier.isEmpty() ? null : categoryIdentifier)
                            .setThreadId(collapseKey)
                            .setContentAvailable(true)
                            .setMutableContent(true)
                            .setSound("default")
                            .build())
                    .build());
        }

        Message message = messageBuilder.build();

        try {
            String messageId = FCM_RETRY.execute(ctx -> {
                try {
                    return FirebaseMessaging.getInstance().send(message);
                } catch (FirebaseMessagingException e) {
                    MessagingErrorCode code = e.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                        log.warn("FCM stale token, removing device: recipientId={}, deviceId={}, code={}",
                                recipientId, device.getId(), code);
                        userDeviceRepository.delete(device);
                        return null;
                    }
                    log.warn("FCM transient error [attempt {}]: recipientId={}, deviceId={}, error={}",
                            ctx.getRetryCount() + 1, recipientId, device.getId(), e.getMessage());
                    throw new RuntimeException("FCM transient error: " + e.getMessage(), e);
                }
            });
            if (messageId != null) {
                log.info("FCM sent: recipientId={}, deviceId={}, messageId={}, actorCount={}",
                        recipientId, device.getId(), messageId, actorCount);
            }
        } catch (Exception e) {
            log.error("FCM failed after max retries: recipientId={}, deviceId={}, error={}",
                    recipientId, device.getId(), e.getMessage());
        }
    }

    private String getStr(Notification n, String key) {
        if (n.getPayload() == null) return null;
        Object v = n.getPayload().get(key);
        return v != null ? v.toString() : null;
    }
}
