package com.example.chatbackend.service;

import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import com.example.chatbackend.session.ChatSessionInitializedEvent;
import com.example.chatbackend.websocket.dto.SessionInitPayload;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class SessionInitializationNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, SessionInitPayload> pendingInitializationsByUserId = new ConcurrentHashMap<>();

    public SessionInitializationNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onChatSessionInitialized(ChatSessionInitializedEvent event) {
        SessionInitPayload payload = new SessionInitPayload(
                event.sessionId(),
                event.sessionKey(),
                event.encryptionEnabled()
        );

        pendingInitializationsByUserId.put(event.userATempUserId(), payload);
        pendingInitializationsByUserId.put(event.userBTempUserId(), payload);

        sendInitialization(event.userATempUserId(), payload);
        sendInitialization(event.userBTempUserId(), payload);
    }

    @EventListener
    public void onSessionInitSubscription(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null || !destination.endsWith("/queue/session.init")) {
            return;
        }

        SessionInitPayload payload = pendingInitializationsByUserId.get(user.getName());
        if (payload != null) {
            sendInitialization(user.getName(), payload);
        }
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        pendingInitializationsByUserId.entrySet()
                .removeIf(entry -> entry.getValue().sessionId().equals(event.sessionId()));
    }

    private void sendInitialization(String tempUserId, SessionInitPayload payload) {
        messagingTemplate.convertAndSendToUser(tempUserId, "/queue/session.init", payload);
    }
}
