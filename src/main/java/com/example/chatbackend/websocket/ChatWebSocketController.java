package com.example.chatbackend.websocket;

import com.example.chatbackend.service.ChatMessageRoutingResult;
import com.example.chatbackend.service.ChatService;
import com.example.chatbackend.service.TypingEventRoutingResult;
import com.example.chatbackend.websocket.dto.ChatMessageInboundPayload;
import com.example.chatbackend.websocket.dto.TypingEventInboundPayload;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Valid ChatMessageInboundPayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Optional<ChatMessageRoutingResult> result = chatService.routeIncomingMessage(headerAccessor.getSessionId(), payload);
        result.ifPresent(value -> messagingTemplate.convertAndSend(sessionTopic(value.sessionId()), value.payload()));
    }

    @MessageMapping("/chat.typing")
    public void typingEvent(
            TypingEventInboundPayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Optional<TypingEventRoutingResult> result = chatService.routeTypingEvent(headerAccessor.getSessionId(), payload);
        result.ifPresent(value -> messagingTemplate.convertAndSend(sessionTopic(value.sessionId()), value.payload()));
    }

    private String sessionTopic(String sessionId) {
        return "/topic/session." + sessionId;
    }
}
