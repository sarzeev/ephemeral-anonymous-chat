package com.example.chatbackend.model;

import java.time.Instant;

public record TemporaryIdentity(
        String temporaryUserId,
        Instant issuedAt,
        Instant expiresAt
) {
}

