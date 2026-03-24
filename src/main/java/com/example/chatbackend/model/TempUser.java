package com.example.chatbackend.model;

import java.time.Instant;

public record TempUser(
        String tempUserId,
        String joinTokenHash,
        String websocketSessionId,
        Instant createdAt,
        Instant lastSeen
) {

    public TempUser withLastSeen(Instant updatedLastSeen) {
        return new TempUser(tempUserId, joinTokenHash, websocketSessionId, createdAt, updatedLastSeen);
    }

    public TempUser withWebSocketSession(String updatedWebsocketSessionId, Instant updatedLastSeen) {
        return new TempUser(tempUserId, joinTokenHash, updatedWebsocketSessionId, createdAt, updatedLastSeen);
    }
}
