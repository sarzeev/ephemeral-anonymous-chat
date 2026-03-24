package com.example.chatbackend.websocket;

import com.example.chatbackend.session.AnonymousSessionManager;
import com.example.chatbackend.session.WebSocketSessionTracker;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketSessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionEventListener.class);

    private final WebSocketSessionTracker webSocketSessionTracker;
    private final AnonymousSessionManager anonymousSessionManager;

    public WebSocketSessionEventListener(
            WebSocketSessionTracker webSocketSessionTracker,
            AnonymousSessionManager anonymousSessionManager
    ) {
        this.webSocketSessionTracker = webSocketSessionTracker;
        this.anonymousSessionManager = anonymousSessionManager;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String websocketSessionId = accessor.getSessionId();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (websocketSessionId == null || sessionAttributes == null) {
            log.warn("Skipping WebSocket connect tracking because sessionId is missing");
            return;
        }

        String tempUserId = (String) sessionAttributes.get(AnonymousChatHandshakeInterceptor.TEMP_USER_ID_ATTRIBUTE);
        String joinTokenHash = (String) sessionAttributes.get(AnonymousChatHandshakeInterceptor.JOIN_TOKEN_HASH_ATTRIBUTE);
        if (tempUserId == null || joinTokenHash == null) {
            log.warn("Skipping WebSocket connection because authorized handshake attributes are missing");
            return;
        }

        webSocketSessionTracker.registerSession(websocketSessionId, tempUserId);
        anonymousSessionManager.onWebSocketConnected(websocketSessionId, tempUserId, joinTokenHash);
        log.info("Tracked WebSocket connection sessionId={} tempUserId={}", websocketSessionId, tempUserId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String websocketSessionId = event.getSessionId();
        Optional<String> tempUserId = webSocketSessionTracker.unregisterSession(websocketSessionId);

        tempUserId.ifPresentOrElse(
                value -> {
                    anonymousSessionManager.onWebSocketDisconnected(websocketSessionId, value);
                    log.info("Tracked WebSocket disconnection sessionId={} tempUserId={}", websocketSessionId, value);
                },
                () -> log.info("Tracked WebSocket disconnection sessionId={} tempUserId=unknown", websocketSessionId)
        );
    }
}
