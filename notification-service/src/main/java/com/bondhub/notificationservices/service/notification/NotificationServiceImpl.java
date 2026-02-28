package com.bondhub.notificationservices.service.notification;

import com.bondhub.common.utils.LocalizationUtil;
import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.notificationservices.dto.request.notification.CreateFriendRequestNotificationRequest;
import com.bondhub.notificationservices.dto.response.notification.NotificationAcceptedResponse;
import com.bondhub.notificationservices.dto.response.notification.NotificationHistoryResponse;
import com.bondhub.notificationservices.dto.response.notification.NotificationResponse;
import com.bondhub.notificationservices.dto.response.notification.UserNotificationStateResponse;
import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.event.RawNotificationEvent;
import com.bondhub.notificationservices.mapper.NotificationMapper;
import com.bondhub.notificationservices.model.Notification;
import com.bondhub.notificationservices.model.UserNotificationState;
import com.bondhub.notificationservices.publisher.RawNotificationPublisher;
import com.bondhub.notificationservices.repository.UserNotificationStateRepository;
import com.bondhub.notificationservices.service.notification.assembler.NotificationAssemblerResolver;
import com.bondhub.notificationservices.service.notificationtemplate.NotificationTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    static final Duration FRESH_WINDOW = Duration.ofHours(2);

    NotificationAssemblerResolver assemblerResolver;
    RawNotificationPublisher rawPublisher;
    MongoTemplate mongoTemplate;
    NotificationMapper notificationMapper;
    SecurityUtil securityUtil;
    LocalizationUtil localizationUtil;
    NotificationTemplateService templateService;
    UserNotificationStateRepository userStateRepository;

    @Override
    public NotificationAcceptedResponse createFriendRequestNotification(
            CreateFriendRequestNotificationRequest request) {
        return enqueue(NotificationType.FRIEND_REQUEST, request);
    }


    @Override
    public NotificationHistoryResponse getNotificationHistory(LocalDateTime cursor, int limit) {
        String userId = securityUtil.getCurrentUserId();
        String locale = localizationUtil.getCurrentLocale();

        Query query = new Query(Criteria.where("userId").is(userId));

        if (cursor != null) {
            query.addCriteria(Criteria.where("lastModifiedAt").lt(cursor));
        }

        query.with(Sort.by(Sort.Direction.DESC, "lastModifiedAt")).limit(limit);

        List<Notification> notifications = mongoTemplate.find(query, Notification.class);

        return groupAndRender(notifications, locale);
    }

    private NotificationHistoryResponse groupAndRender(List<Notification> notifications, String locale) {
        if (notifications.isEmpty()) {
            return new NotificationHistoryResponse(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursAgo = now.minus(FRESH_WINDOW);
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();

        List<NotificationResponse> newest = new ArrayList<>();
        List<NotificationResponse> today = new ArrayList<>();
        List<NotificationResponse> previous = new ArrayList<>();

        for (Notification n : notifications) {
            NotificationResponse res = convertToResponse(n, locale);
            LocalDateTime time = n.getLastModifiedAt();

            if (time.isAfter(twoHoursAgo)) {
                newest.add(res);
            } else if (time.isAfter(startOfToday)) {
                today.add(res);
            } else {
                previous.add(res);
            }
        }

        return buildResponse(newest, today, previous, notifications);
    }

    @Override
    public UserNotificationStateResponse getNotificationState() {
        String userId = securityUtil.getCurrentUserId();
        UserNotificationState state = userStateRepository.findById(userId)
                .orElse(UserNotificationState.builder()
                        .userId(userId)
                        .unreadCount(0)
                        .build());

        return notificationMapper.toStateResponse(state);
    }

    @Override
    public void markHistoryAsChecked() {
        String userId = securityUtil.getCurrentUserId();
        mongoTemplate.upsert(
                new Query(Criteria.where("userId").is(userId)),
                new Update().set("lastCheckedAt", LocalDateTime.now()).set("unreadCount", 0L),
                UserNotificationState.class
        );
    }

    @Override
    public void markAsRead(String id) {
        String userId = securityUtil.getCurrentUserId();
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("_id").is(id)
                .and("isRead").is(false));

        Update update = new Update()
                .set("isRead", true)
                .set("readAt", LocalDateTime.now());

        mongoTemplate.findAndModify(query, update, Notification.class);
    }

    @Override
    public void markAllAsRead() {
        String userId = securityUtil.getCurrentUserId();
        Query query = new Query((Criteria) Criteria.where("userId").is(userId)
                .and("isRead").is(false));

        Update update = new Update()
                .set("isRead", true)
                .set("readAt", LocalDateTime.now());

        mongoTemplate.updateMulti(query, update, Notification.class);
    }

    private NotificationHistoryResponse buildResponse(
            List<NotificationResponse> newest,
            List<NotificationResponse> today,
            List<NotificationResponse> previous,
            List<Notification> source
    ) {
        LocalDateTime nextCursor = source.isEmpty() ? null : source.getLast().getLastModifiedAt();
        return new NotificationHistoryResponse(
                newest != null ? newest : new ArrayList<>(),
                today != null ? today : new ArrayList<>(),
                previous != null ? previous : new ArrayList<>(),
                nextCursor
        );
    }

    private NotificationResponse convertToResponse(Notification n, String locale) {
        String title = templateService.renderTitle(
                n.getType(),
                NotificationChannel.IN_APP,
                locale,
                n.getData()
        );

        String body = templateService.renderBody(
                n.getType(),
                NotificationChannel.IN_APP,
                locale,
                n.getData()
        );

        return notificationMapper.toResponse(n, title, body);
    }

    private NotificationAcceptedResponse enqueue(NotificationType type, Object request) {
        RawNotificationEvent event = assemblerResolver.get(type).build(request);
        log.info("[API] Enqueueing notification: type={}, recipient={}", type, event.getRecipientId());
        rawPublisher.publish(event);
        return NotificationAcceptedResponse.queued();
    }
}
