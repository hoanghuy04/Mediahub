package com.bondhub.notificationservices.service.delivery.handler;

import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.model.UserDevice;
import com.bondhub.notificationservices.repository.UserDeviceRepository;
import com.bondhub.notificationservices.service.notificationtemplate.NotificationTemplateService;
import com.bondhub.notificationservices.service.preference.UserPreferenceService;
import com.bondhub.notificationservices.service.presence.PresenceService;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
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

        String title = templateService.renderTitle(persisted.getType(), NotificationChannel.FCM, locale, renderData);
        String body = templateService.renderBody(persisted.getType(), NotificationChannel.FCM, locale, renderData);

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

        Message message = Message.builder()
                .setToken(device.getFcmToken())
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setCollapseKey(collapseKey)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setImage(lastActorAvatar != null ? lastActorAvatar : "default_icon")
                                .setImage(lastActorAvatar)
                                .setChannelId("default")
                                .setSound("default")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .setSound("default")
                                .setCategory(categoryIdentifier.isEmpty() ? null : categoryIdentifier)
                                .setThreadId(collapseKey)
                                .setMutableContent(true)
                                .build())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setIcon(lastActorAvatar != null ? lastActorAvatar : "/images/logo.png")
                                .setBadge("/images/logo.png")
                                .setTag(collapseKey)
                                .build())
                        .build())
                .putData("type", type)
                .putData("title", title != null ? title : "")
                .putData("body", body != null ? body : "")
                .putData("actorId", lastActorId != null ? lastActorId : "")
                .putData("actorName", lastActorName != null ? lastActorName : "")
                .putData("actorAvatar", lastActorAvatar != null ? lastActorAvatar : "")
                .putData("actorCount", String.valueOf(actorCount))
                .putData("othersCount", String.valueOf(othersCount))
                .putData("categoryIdentifier", categoryIdentifier)
                .putData("requestId", requestId != null ? requestId : "")
                .build();

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
