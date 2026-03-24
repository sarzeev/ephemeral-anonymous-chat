package com.example.chatbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityHardeningProperties {

    private long joinTokenTtlSeconds = 120;
    private int maxJoinAttemptsPerIpPerMinute = 30;

    public long getJoinTokenTtlSeconds() {
        return joinTokenTtlSeconds;
    }

    public void setJoinTokenTtlSeconds(long joinTokenTtlSeconds) {
        this.joinTokenTtlSeconds = joinTokenTtlSeconds;
    }

    public int getMaxJoinAttemptsPerIpPerMinute() {
        return maxJoinAttemptsPerIpPerMinute;
    }

    public void setMaxJoinAttemptsPerIpPerMinute(int maxJoinAttemptsPerIpPerMinute) {
        this.maxJoinAttemptsPerIpPerMinute = maxJoinAttemptsPerIpPerMinute;
    }
}

