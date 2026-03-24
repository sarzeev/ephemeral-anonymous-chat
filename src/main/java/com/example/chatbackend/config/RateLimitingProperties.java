package com.example.chatbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingProperties {

    private boolean enabled = false;
    private int requestsPerMinute = 60;
    private int burstCapacity = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
    }
}

