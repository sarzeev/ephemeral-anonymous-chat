package com.example.chatbackend.service;

import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SessionMessagePurgeListener {

    private final MessageLifecycleManager messageLifecycleManager;

    public SessionMessagePurgeListener(MessageLifecycleManager messageLifecycleManager) {
        this.messageLifecycleManager = messageLifecycleManager;
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        messageLifecycleManager.purgeSessionMessages(event.sessionId());
    }
}

