package com.example.chatbackend.service;

import com.example.chatbackend.websocket.dto.ChatMessageOutboundPayload;

public record ChatMessageRoutingResult(
        String sessionId,
        ChatMessageOutboundPayload payload
) {
}

