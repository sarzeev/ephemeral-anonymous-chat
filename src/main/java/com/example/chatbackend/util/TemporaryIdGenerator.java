package com.example.chatbackend.util;

import java.util.UUID;

public final class TemporaryIdGenerator {

    private TemporaryIdGenerator() {
    }

    public static String generate() {
        return "temp-" + UUID.randomUUID();
    }
}

