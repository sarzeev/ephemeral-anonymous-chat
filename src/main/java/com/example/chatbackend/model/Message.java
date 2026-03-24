package com.example.chatbackend.model;

import java.time.Instant;

public record Message(
        String messageId,
        String sessionId,
        String senderId,
        String encryptedPayload,
        String iv,
        Instant createdAt,
        Instant expiryTime,
        String hmacSignature
) {

    public Message withSecurityMetadata(
            Instant updatedCreatedAt,
            Instant updatedExpiryTime,
            String updatedHmacSignature
    ) {
        return new Message(
                messageId,
                sessionId,
                senderId,
                encryptedPayload,
                iv,
                updatedCreatedAt,
                updatedExpiryTime,
                updatedHmacSignature
        );
    }
}
