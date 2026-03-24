package com.example.chatbackend.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionTracker {

    private final ConcurrentMap<String, String> sessionsByWebSocketId = new ConcurrentHashMap<>();

    public void registerSession(String webSocketSessionId, String tempUserId) {
        sessionsByWebSocketId.put(webSocketSessionId, tempUserId);
    }

    public Optional<String> findTempUserId(String webSocketSessionId) {
        return Optional.ofNullable(sessionsByWebSocketId.get(webSocketSessionId));
    }

    public Optional<String> unregisterSession(String webSocketSessionId) {
        return Optional.ofNullable(sessionsByWebSocketId.remove(webSocketSessionId));
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(sessionsByWebSocketId);
    }
}

