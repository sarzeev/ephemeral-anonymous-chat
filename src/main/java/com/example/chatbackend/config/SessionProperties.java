package com.example.chatbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.session")
public class SessionProperties {

    private long cleanupIntervalMs = 30000;
    private int maxActiveSessions = 10000;

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }
}

