package com.example.chatbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private int maxMessagesPerSessionPerSecond = 10;
    private long typingDebounceMs = 500;

    public int getMaxMessagesPerSessionPerSecond() {
        return maxMessagesPerSessionPerSecond;
    }

    public void setMaxMessagesPerSessionPerSecond(int maxMessagesPerSessionPerSecond) {
        this.maxMessagesPerSessionPerSecond = maxMessagesPerSessionPerSecond;
    }

    public long getTypingDebounceMs() {
        return typingDebounceMs;
    }

    public void setTypingDebounceMs(long typingDebounceMs) {
        this.typingDebounceMs = typingDebounceMs;
    }
}

