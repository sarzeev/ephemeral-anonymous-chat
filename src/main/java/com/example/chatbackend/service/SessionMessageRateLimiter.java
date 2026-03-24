package com.example.chatbackend.service;

import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SessionMessageRateLimiter {

    private final ConcurrentMap<String, SessionWindow> windowsBySessionId = new ConcurrentHashMap<>();

    public boolean allowMessage(String sessionId, int maxMessagesPerSecond) {
        long nowMillis = Instant.now().toEpochMilli();
        SessionWindow window = windowsBySessionId.computeIfAbsent(sessionId, ignored -> new SessionWindow());
        return window.tryAcquire(nowMillis, maxMessagesPerSecond);
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        windowsBySessionId.remove(event.sessionId());
    }

    private static final class SessionWindow {

        private long windowStartMillis;
        private int count;

        private synchronized boolean tryAcquire(long nowMillis, int maxMessagesPerSecond) {
            if (windowStartMillis == 0L || nowMillis - windowStartMillis >= 1000) {
                windowStartMillis = nowMillis;
                count = 0;
            }

            if (count >= maxMessagesPerSecond) {
                return false;
            }

            count++;
            return true;
        }
    }
}

