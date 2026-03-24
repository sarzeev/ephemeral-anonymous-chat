package com.example.chatbackend.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class IpJoinAttemptRateLimiter {

    private final ConcurrentMap<String, AttemptWindow> attemptWindows = new ConcurrentHashMap<>();

    public boolean allowAttempt(String ipAddress, int maxAttemptsPerMinute) {
        long nowMillis = Instant.now().toEpochMilli();
        AttemptWindow window = attemptWindows.computeIfAbsent(ipAddress, ignored -> new AttemptWindow());
        return window.tryAcquire(nowMillis, maxAttemptsPerMinute);
    }

    private static final class AttemptWindow {

        private long windowStartMillis;
        private int count;

        private synchronized boolean tryAcquire(long nowMillis, int maxAttemptsPerMinute) {
            if (windowStartMillis == 0L || nowMillis - windowStartMillis >= 60_000L) {
                windowStartMillis = nowMillis;
                count = 0;
            }

            if (count >= maxAttemptsPerMinute) {
                return false;
            }

            count++;
            return true;
        }
    }
}

