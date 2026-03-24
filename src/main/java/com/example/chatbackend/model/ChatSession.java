package com.example.chatbackend.model;

import java.time.Instant;

public record ChatSession(
        String sessionId,
        TempUser userA,
        TempUser userB,
        Instant createdAt,
        Instant lastActivityTime
) {

    public ChatSession withLastActivityTime(Instant updatedLastActivityTime) {
        return new ChatSession(sessionId, userA, userB, createdAt, updatedLastActivityTime);
    }
}

