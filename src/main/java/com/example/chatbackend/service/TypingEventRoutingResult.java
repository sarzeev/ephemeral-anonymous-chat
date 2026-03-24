package com.example.chatbackend.service;

import com.example.chatbackend.websocket.dto.TypingEventOutboundPayload;

public record TypingEventRoutingResult(
        String sessionId,
        TypingEventOutboundPayload payload
) {
}

