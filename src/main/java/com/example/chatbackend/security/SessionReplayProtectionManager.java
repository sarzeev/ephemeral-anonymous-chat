package com.example.chatbackend.security;

import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SessionReplayProtectionManager {

    private final ConcurrentMap<String, Set<String>> messageIdsBySession = new ConcurrentHashMap<>();

    public boolean registerMessageId(String sessionId, String messageId) {
        return messageIdsBySession
                .computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet())
                .add(messageId);
    }

    public void forgetMessageId(String sessionId, String messageId) {
        Set<String> knownMessageIds = messageIdsBySession.get(sessionId);
        if (knownMessageIds == null) {
            return;
        }

        knownMessageIds.remove(messageId);
        if (knownMessageIds.isEmpty()) {
            messageIdsBySession.remove(sessionId, knownMessageIds);
        }
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        messageIdsBySession.remove(event.sessionId());
    }
}

