package com.example.chatbackend.model;

public record TypingEventCommand(
        String roomId,
        boolean typing
) {
}

