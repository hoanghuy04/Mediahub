package com.bondhub.notificationservices.controller;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.notificationservices.dto.request.notification.CreateFriendRequestNotificationRequest;
import com.bondhub.notificationservices.dto.response.notification.NotificationAcceptedResponse;
import com.bondhub.notificationservices.dto.response.notification.NotificationHistoryResponse;
import com.bondhub.notificationservices.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {

    NotificationService notificationService;

    @PostMapping("/friend-request")
    public ResponseEntity<ApiResponse<NotificationAcceptedResponse>> createFriendRequest(
            @Valid @RequestBody CreateFriendRequestNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(notificationService.createFriendRequestNotification(request)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<NotificationHistoryResponse>> getNotificationHistory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursor,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationHistory(cursor, limit)));
    }
}
