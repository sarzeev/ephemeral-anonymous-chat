package com.example.chatbackend.service;

import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TypingEventDebounceManager {

    private final ConcurrentMap<String, Long> lastTypingEventAtByKey = new ConcurrentHashMap<>();

    public boolean shouldPublish(String sessionId, String senderId, long debounceWindowMs) {
        long now = Instant.now().toEpochMilli();
        String key = sessionId + ':' + senderId;
        return lastTypingEventAtByKey.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= debounceWindowMs) {
                return now;
            }
            return previous;
        }) == now;
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        String prefix = event.sessionId() + ':';
        lastTypingEventAtByKey.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
