package com.example.chatbackend.session;

public record ChatSessionInitializedEvent(
        String sessionId,
        String userATempUserId,
        String userBTempUserId,
        String sessionKey,
        boolean encryptionEnabled
) {
}

