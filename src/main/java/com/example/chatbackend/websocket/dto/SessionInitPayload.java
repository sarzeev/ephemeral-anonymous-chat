package com.example.chatbackend.websocket.dto;

public record SessionInitPayload(
        String sessionId,
        String sessionKey,
        boolean encryptionEnabled
) {
}

