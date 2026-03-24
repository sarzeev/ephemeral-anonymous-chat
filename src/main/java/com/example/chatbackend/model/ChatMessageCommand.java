package com.example.chatbackend.model;

public record ChatMessageCommand(
        String roomId,
        String content
) {
}

