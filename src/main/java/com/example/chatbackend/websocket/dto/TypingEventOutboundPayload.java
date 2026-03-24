package com.example.chatbackend.websocket.dto;

import java.time.Instant;

public record TypingEventOutboundPayload(
        String senderId,
        boolean typing,
        Instant createdAt
) {
}

