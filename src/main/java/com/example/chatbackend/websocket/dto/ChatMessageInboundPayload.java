package com.example.chatbackend.websocket.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageInboundPayload(
        @NotBlank String messageId,
        @NotBlank String encryptedPayload,
        @NotBlank String iv,
        @NotBlank String hmacSignature
) {
}
