package com.bondhub.messageservice.controller;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.messageservice.model.ChatUser;
import com.bondhub.messageservice.service.UserPresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/presence")
@Tag(name = "User Presence", description = "User online/offline status API")
public class UserPresenceController {

    private final UserPresenceService userPresenceService;

    @MessageMapping("/user.addUser")
    @SendTo("/topic/public")
    public ChatUser addUser(
            @Payload ChatUser user,
            SimpMessageHeaderAccessor headerAccessor) {
        ChatUser savedUser = userPresenceService.saveUser(user);
        headerAccessor.getSessionAttributes().put("userId", savedUser.getId());
        return savedUser;
    }

    @MessageMapping("/user.disconnectUser")
    @SendTo("/topic/public")
    public ChatUser disconnectUser(
            @Payload ChatUser user) {
        userPresenceService.disconnect(user.getId());
        return user;
    }

    @GetMapping("/online")
    @Operation(summary = "Get list of online users")
    public ResponseEntity<ApiResponse<List<ChatUser>>> findConnectedUsers() {
        return ResponseEntity.ok(ApiResponse.success(
                userPresenceService.findConnectedUsers()
        ));
    }
}
