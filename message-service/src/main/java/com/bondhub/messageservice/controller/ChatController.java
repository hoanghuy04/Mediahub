package com.bondhub.messageservice.controller;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.messageservice.model.ChatMessage;
import com.bondhub.messageservice.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Real-time chat API")
public class ChatController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        // Principal.getName() is set to userId in WebSocketConfig
        chatMessage.setSenderId(principal.getName());
        chatMessageService.sendMessage(chatMessage);
    }

    @GetMapping("/messages/{recipientId}")
    @Operation(summary = "Get chat messages by recipient ID")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> findChatMessages(@PathVariable String recipientId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatMessageService.findChatMessages(recipientId)));
    }
}
