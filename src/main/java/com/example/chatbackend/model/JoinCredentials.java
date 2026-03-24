package com.example.chatbackend.model;

public record JoinCredentials(
        String tempUserId,
        String joinToken
) {
}
