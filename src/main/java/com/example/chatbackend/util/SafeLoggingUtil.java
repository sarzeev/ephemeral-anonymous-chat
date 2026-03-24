package com.example.chatbackend.util;

public final class SafeLoggingUtil {

    private SafeLoggingUtil() {
    }

    public static String redactMessageContent() {
        return "[REDACTED_MESSAGE_CONTENT]";
    }
}

