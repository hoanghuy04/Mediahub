# Notification Service — Current State

> Snapshot code hiện tại **trước khi** implement BH-147 (Notification Batcher).  
> Bỏ qua: `dto/`, `mapper/`, `config/`, `repository/`, `model/`, `enums/`, `event/`

---

## Package Structure

```
com.bondhub.notificationservices/
├── controller/
│   ├── DeviceController.java
│   ├── NotificationController.java
│   └── NotificationTemplateController.java
├── orchestrator/
│   └── NotificationOrchestrator.java
├── service/
│   ├── device/
│   │   ├── DeviceService.java
│   │   └── DeviceServiceImpl.java
│   ├── frequency/
│   │   ├── FrequencyControlService.java
│   │   └── FrequencyControlServiceImpl.java
│   ├── notification/
│   │   ├── NotificationService.java
│   │   └── NotificationServiceImpl.java
│   ├── notificationtemplate/
│   │   ├── NotificationTemplateService.java
│   │   └── NotificationTemplateServiceImpl.java
│   └── preference/
│       ├── UserPreferenceService.java
│       └── UserPreferenceServiceImpl.java   ⚠️ EMPTY
├── strategy/
│   └── content/
│       ├── NotificationContentStrategy.java
│       ├── FriendRequestContentStrategy.java
│       └── factory/
│           └── ContentStrategyFactory.java
└── utils/
    └── TemplateEngine.java
```

---

## Request Flow

```
POST /notifications/friend-request
        │
        ▼
NotificationController
        │ createFriendRequestNotification(request)
        ▼
NotificationServiceImpl
        │ orchestrator.process(FRIEND_REQUEST, request)
        ▼
NotificationOrchestrator
        │ contentStrategyFactory.get(FRIEND_REQUEST)
        │         ↓
        │  FriendRequestContentStrategy.build(request)
        │         │ templateService.renderTitle(type, locale, data)
        │         │ templateService.renderBody(type, locale, data)
        │         │         ↓
        │         │   NotificationTemplateServiceImpl
        │         │         │ repo.findByTypeAndChannelAndLocaleAndActiveTrue()
        │         │         │ templateEngine.render(template, data)
        │         │         ↓
        │         │   TemplateEngine.render("{{senderName}} đã ...", data)
        │         ▼
        │  → Notification { title, body, userId, type, referenceId }
        │
        ▼
notificationRepository.save(notification)
        │
        ▼
NotificationResponse (trả về HTTP)
```

---

## Controllers

### `DeviceController.java`

```java
package com.bondhub.notificationservices.controller;

@RestController
@RequestMapping("/notifications/devices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceController {

    DeviceService deviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> registerDevice(
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceService.registerDevice(request);
        return ResponseEntity.ok(ApiResponse.success("Device registered successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> unregisterDevice(
            @RequestParam String userId,
            @RequestParam String token) {
        deviceService.unregisterDevice(userId, token);
        return ResponseEntity.ok(ApiResponse.success("Device unregistered successfully"));
    }
}
```

---

### `NotificationController.java`

```java
package com.bondhub.notificationservices.controller;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    @PostMapping("/friend-request")
    public ResponseEntity<ApiResponse<NotificationResponse>> createFriendRequest(
            @Valid @RequestBody CreateFriendRequestNotificationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(notificationService.createFriendRequestNotification(request)));
    }
}
```

---

### `NotificationTemplateController.java`

```java
package com.bondhub.notificationservices.controller;

@RestController
@RequestMapping("/notification-templates")
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateService service;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> create(
            @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> get(
            @RequestParam NotificationType type,
            @RequestParam String locale) {
        // ⚠️ THIẾU @RequestParam channel — gọi service.getTemplate(type, locale) sẽ lỗi
        //    vì method signature là getTemplate(type, channel, locale)
        return ResponseEntity.ok(ApiResponse.success(service.getTemplate(type, locale)));
    }
}
```

---

## Orchestrator

### `NotificationOrchestrator.java`

```java
package com.bondhub.notificationservices.orchestrator;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationOrchestrator {

    ContentStrategyFactory contentStrategyFactory;
    NotificationRepository notificationRepository;

    public Notification process(NotificationType type, Object request) {
        log.info("Orchestrator processing notification type={}", type);
        NotificationContentStrategy strategy = contentStrategyFactory.get(type);
        Notification notification = strategy.build(request);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved id={}", saved.getId());
        return saved;
    }
}
```

---

## Services

### `NotificationService.java`

```java
package com.bondhub.notificationservices.service.notification;

public interface NotificationService {
    NotificationResponse createFriendRequestNotification(
            CreateFriendRequestNotificationRequest request);
}
```

### `NotificationServiceImpl.java`

```java
package com.bondhub.notificationservices.service.notification;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationOrchestrator orchestrator;
    NotificationMapper mapper;

    // ⚠️ unused imports: ContentStrategyFactory, NotificationContentStrategy,
    //                     NotificationRepository (đã bị orchestrator hấp thụ)

    @Override
    public NotificationResponse createFriendRequestNotification(
            CreateFriendRequestNotificationRequest request) {
        log.info("Creating friend request notification");
        Notification saved = orchestrator.process(NotificationType.FRIEND_REQUEST, request);
        return mapper.toResponse(saved);
    }
}
```

---

### `DeviceService.java`

```java
package com.bondhub.notificationservices.service.device;

public interface DeviceService {
    void registerDevice(DeviceTokenRequest request);
    void unregisterDevice(String userId, String token);
    List<UserDevice> getDevicesForUser(String userId);
}
```

### `DeviceServiceImpl.java`

```java
package com.bondhub.notificationservices.service.device;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    UserDeviceRepository userDeviceRepository;

    @Override
    @Transactional
    public void registerDevice(DeviceTokenRequest request) {
        // idempotent: chỉ save nếu chưa tồn tại cặp (userId, fcmToken)
        userDeviceRepository.findByUserIdAndFcmToken(request.userId(), request.token())
                .ifPresentOrElse(
                        existing -> log.info("Device already exists for user: {}", request.userId()),
                        () -> {
                            UserDevice device = UserDevice.builder()
                                    .userId(request.userId())
                                    .fcmToken(request.token())
                                    .platform(request.platform())
                                    .build();
                            userDeviceRepository.save(device);
                        }
                );
    }

    @Override
    @Transactional
    public void unregisterDevice(String userId, String token) {
        userDeviceRepository.deleteByUserIdAndFcmToken(userId, token);
    }

    @Override
    public List<UserDevice> getDevicesForUser(String userId) {
        return userDeviceRepository.findByUserId(userId);
    }
}
```

---

### `FrequencyControlService.java`

```java
package com.bondhub.notificationservices.service.frequency;

public interface FrequencyControlService {
    boolean allow(String userId, NotificationType type);
}
```

### `FrequencyControlServiceImpl.java`

```java
package com.bondhub.notificationservices.service.frequency;

@Service
@RequiredArgsConstructor
public class FrequencyControlServiceImpl implements FrequencyControlService {

    private final NotificationRepository repository;
    private static final int MAX_PER_MINUTE = 5;

    @Override
    public boolean allow(String userId, NotificationType type) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long count = repository.countByUserIdAndTypeAndCreatedAtAfter(userId, type, oneMinuteAgo);
        return count < MAX_PER_MINUTE;
    }
}
// ⚠️ DEAD CODE — không ai inject hay gọi service này
```

---

### `UserPreferenceService.java`

```java
package com.bondhub.notificationservices.service.preference;

public interface UserPreferenceService {
    boolean allow(String userId, NotificationType type);
}
```

### `UserPreferenceServiceImpl.java`

```java
package com.bondhub.notificationservices.service.preference;

public class UserPreferenceServiceImpl {
    // ⚠️ EMPTY — chưa implement, không có @Service annotation
}
```

---

### `NotificationTemplateService.java`

```java
package com.bondhub.notificationservices.service.notificationtemplate;

public interface NotificationTemplateService {
    NotificationTemplateResponse create(CreateTemplateRequest request);
    NotificationTemplateResponse update(String id, UpdateTemplateRequest request);
    NotificationTemplateResponse getTemplate(NotificationType type, NotificationChannel channel, String locale);
    String renderTitle(NotificationType type, NotificationChannel channel, String locale, Map<String, Object> data);
    String renderBody(NotificationType type, NotificationChannel channel, String locale, Map<String, Object> data);
}
```

### `NotificationTemplateServiceImpl.java`

```java
package com.bondhub.notificationservices.service.notificationtemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    NotificationTemplateRepository notificationTemplateRepository;
    TemplateEngine templateEngine;
    NotificationTemplateMapper notificationTemplateMapper;

    @Override
    public NotificationTemplateResponse create(CreateTemplateRequest request) {
        NotificationTemplate template = notificationTemplateMapper.toEntity(request);
        template.setActive(true);
        notificationTemplateRepository.save(template);
        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public NotificationTemplateResponse update(String id, UpdateTemplateRequest request) {
        NotificationTemplate template = notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));
        if (request.titleTemplate() != null) template.setTitleTemplate(request.titleTemplate());
        if (request.bodyTemplate() != null)  template.setBodyTemplate(request.bodyTemplate());
        if (request.active() != null)        template.setActive(request.active());
        notificationTemplateRepository.save(template);
        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public NotificationTemplateResponse getTemplate(
            NotificationType type, NotificationChannel channel, String locale) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));
        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public String renderTitle(NotificationType type, NotificationChannel channel,
                              String locale, Map<String, Object> data) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow();
        return templateEngine.render(template.getTitleTemplate(), data);
    }

    @Override
    public String renderBody(NotificationType type, NotificationChannel channel,
                             String locale, Map<String, Object> data) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow();
        return templateEngine.render(template.getBodyTemplate(), data);
    }
}
```

---

## Strategy

### `NotificationContentStrategy.java`

```java
package com.bondhub.notificationservices.strategy.content;

public interface NotificationContentStrategy {
    NotificationType getType();
    Notification build(Object request);
    // BH-147: build() sẽ đổi return type → RawNotificationEvent
}
```

### `FriendRequestContentStrategy.java`

```java
package com.bondhub.notificationservices.strategy.content;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendRequestContentStrategy implements NotificationContentStrategy {

    NotificationTemplateService templateService;

    @Override
    public NotificationType getType() {
        return NotificationType.FRIEND_REQUEST;
    }

    @Override
    public Notification build(Object request) {
        CreateFriendRequestNotificationRequest req =
                (CreateFriendRequestNotificationRequest) request;

        Map<String, Object> data = Map.of(
                "senderName", req.senderName(),
                "senderId",   req.senderId(),
                "requestId",  req.requestId()
        );

        // ⚠️ gọi renderTitle/renderBody(type, locale, data) — THIẾU channel param
        //    sẽ compile error vì interface yêu cầu (type, channel, locale, data)
        String title = templateService.renderTitle(NotificationType.FRIEND_REQUEST, req.locale(), data);
        String body  = templateService.renderBody(NotificationType.FRIEND_REQUEST, req.locale(), data);

        return Notification.builder()
                .userId(req.receiverId())
                .type(NotificationType.FRIEND_REQUEST)
                .referenceId(req.requestId())
                .title(title)
                .body(body)
                .data(data)
                .isRead(false)
                .build();
    }
}
```

### `factory/ContentStrategyFactory.java`

```java
package com.bondhub.notificationservices.strategy.content.factory;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContentStrategyFactory {

    List<NotificationContentStrategy> strategies;

    public NotificationContentStrategy get(NotificationType type) {
        return strategies.stream()
                .filter(s -> s.getType() == type)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_STRATEGY_NOT_FOUND));
    }
}
```

---

## Utils

### `TemplateEngine.java`

```java
package com.bondhub.notificationservices.utils;

@Component
public class TemplateEngine {

    public String render(String template, Map<String, Object> data) {
        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
        }
        return result;
    }
}
// Ví dụ: render("{{senderName}} đã gửi lời mời", {"senderName": "An"})
//      → "An đã gửi lời mời"
```

---

## Bugs / Vấn đề cần xử lý trước BH-147

| #   | Mức độ       | File                             | Vấn đề                                                                                            |
| --- | ------------ | -------------------------------- | ------------------------------------------------------------------------------------------------- |
| 1   | 🔴 BUG       | `FriendRequestContentStrategy`   | Gọi `renderTitle(type, locale, data)` — thiếu `channel` → **compile error**                       |
| 2   | 🔴 BUG       | `NotificationTemplateController` | `@GetMapping` gọi `getTemplate(type, locale)` — thiếu `channel` → **compile error**               |
| 3   | 🟡 EMPTY     | `UserPreferenceServiceImpl`      | Class rỗng, không có `@Service`                                                                   |
| 4   | 🟡 DEAD CODE | `FrequencyControlService`        | Không ai inject hay gọi                                                                           |
| 5   | 🟢 CLEANUP   | `NotificationServiceImpl`        | Unused imports: `ContentStrategyFactory`, `NotificationContentStrategy`, `NotificationRepository` |

---

## Những gì BH-147 sẽ thay đổi

| File                                    | Thay đổi                                                                                    |
| --------------------------------------- | ------------------------------------------------------------------------------------------- |
| `NotificationContentStrategy.java`      | `build()` đổi return type `Notification` → `RawNotificationEvent`                           |
| `FriendRequestContentStrategy.java`     | Sửa `build()` — chỉ extract data, không render title/body                                   |
| `NotificationOrchestrator.java`         | Không save MongoDB trực tiếp → publish `RawNotificationEvent` vào Kafka `raw-notifications` |
| `NotificationServiceImpl.java`          | Giữ nguyên flow, chỉ thay đổi gián tiếp qua Orchestrator                                    |
| `TemplateEngine.java`                   | Giữ nguyên, được dùng bởi `BatchFlushService`                                               |
| `NotificationTemplateService/Impl.java` | Thêm `channel` vào các method còn thiếu, mở rộng cho batched rendering                      |
