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
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
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

    @NonFinal
    @Value("${bondhub.frontend-url}")
    String frontendUrl;

    public void push(Notification persisted) {
        String recipientId = persisted.getUserId();

//        if (presenceService.isOnline(recipientId)) {
//            log.debug("FCM skip: user is online, recipientId={}", recipientId);
//            return;
//        }

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

        Map<String, Object> baseRenderData = new HashMap<>(persisted.getPayload() != null ? persisted.getPayload() : Collections.emptyMap());
        baseRenderData.put("actorCount", actorCount);
        baseRenderData.put("othersCount", actorCount > 2 ? actorCount - 1 : 0);
        baseRenderData.put("showSecondActor", actorCount == 2);
        baseRenderData.put("actorName", lastActorName != null ? lastActorName : "");
        baseRenderData.put("actorAvatar", lastActorAvatar != null ? lastActorAvatar : "");

        if (actorCount == 2) {
            String secondActorName = getStr(persisted, "secondActorName");
            baseRenderData.put("secondActorName", secondActorName != null ? secondActorName : "một người khác");
        }

        String collapseKey = persisted.getType().name() + "_" + recipientId;

        for (UserDevice device : devices) {
            String deviceLocale = device.getLocale();
            
            if (deviceLocale == null) {
                var userPrefs = userPreferenceService.getInternalPreferences(recipientId);
                String globalLocale = userPrefs != null ? userPrefs.getLanguage() : "vi";
                Map<String, String> deviceLocales = userPrefs != null && userPrefs.getLanguageByDeviceId() != null 
                        ? userPrefs.getLanguageByDeviceId() 
                        : Map.of();
                deviceLocale = deviceLocales.getOrDefault(device.getDeviceId(), globalLocale);
            }
            
            if (deviceLocale == null) deviceLocale = "vi";

            // Tạo bản copy renderData cho từng thiết bị để xử lý đa ngôn ngữ
            Map<String, Object> deviceRenderData = new HashMap<>(baseRenderData);
            
            // Nếu payload có chứa bản dịch riêng cho locale của thiết bị này, hãy ưu tiên sử dụng
            String localeKey = deviceLocale.toLowerCase();
            if (deviceRenderData.containsKey("message_" + localeKey)) {
                deviceRenderData.put("message", deviceRenderData.get("message_" + localeKey));
            }
            if (deviceRenderData.containsKey("title_" + localeKey)) {
                deviceRenderData.put("title", deviceRenderData.get("title_" + localeKey));
            }

            var template = templateService.getTemplate(persisted.getType(), NotificationChannel.FCM, deviceLocale);
            String title = templateService.render(template.titleTemplate(), deviceRenderData);
            String body = templateService.render(template.bodyTemplate(), deviceRenderData);

            log.info("FCM processing: type={}, recipientId={}, deviceId={}, locale={}, title='{}', body='{}'",
                    persisted.getType(), recipientId, device.getDeviceId(), deviceLocale, title, body);

            if ("".equals(title) && "".equals(body)) {
                log.warn("FCM skip: both title and body are empty for device={}, type={}", device.getId(), persisted.getType());
                continue;
            }

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

        String url = frontendUrl;
        if (!url.endsWith("/")) url += "/";

        Message.Builder messageBuilder = Message.builder()
                .setToken(device.getFcmToken())
                .putAllData(Map.ofEntries(
                        Map.entry("type", type),
                        Map.entry("title", title != null ? title : ""),
                        Map.entry("body", body != null ? body : ""),
                        Map.entry("actorId", lastActorId != null ? lastActorId : ""),
                        Map.entry("actorName", lastActorName != null ? lastActorName : ""),
                        Map.entry("actorAvatar", lastActorAvatar != null ? lastActorAvatar : ""),
                        Map.entry("actorCount", String.valueOf(actorCount)),
                        Map.entry("othersCount", String.valueOf(othersCount)),
                        Map.entry("categoryIdentifier", categoryIdentifier),
                        Map.entry("requestId", requestId != null ? requestId : ""),
                        Map.entry("url", url)
                ));

        if (device.getPlatform() == Platform.WEB) {
            String iconUrl = lastActorAvatar != null && !lastActorAvatar.isEmpty() ? lastActorAvatar : "/images/logo.jpg";
            log.info("[FCM] Sending data-only message to WEB with icon: {}", iconUrl);
            
            messageBuilder.setWebpushConfig(WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.withLink(url))
                    .build());
        }

        if (device.getPlatform() == Platform.ANDROID || device.getPlatform() == Platform.IOS) {
             messageBuilder.setNotification(com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
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
