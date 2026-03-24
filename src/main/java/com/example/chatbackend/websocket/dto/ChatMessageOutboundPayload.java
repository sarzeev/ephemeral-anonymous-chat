package com.example.chatbackend.websocket.dto;

import java.time.Instant;

public record ChatMessageOutboundPayload(
        String messageId,
        String senderId,
        String encryptedPayload,
        String iv,
        Instant createdAt,
        Instant expiryTime,
        String hmacSignature
) {
}
