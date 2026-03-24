package com.example.chatbackend.service;

import com.example.chatbackend.config.EncryptionProperties;
import com.example.chatbackend.session.ChatSessionTerminatedEvent;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final EncryptionProperties encryptionProperties;
    private final ConcurrentMap<String, String> sessionKeysBySessionId = new ConcurrentHashMap<>();

    public EncryptionService(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
    }

    public String initializeSessionKey(String sessionId) {
        if (!encryptionProperties.isEnabled()) {
            sessionKeysBySessionId.put(sessionId, "");
            return "";
        }

        byte[] rawKey = new byte[32];
        secureRandom.nextBytes(rawKey);
        String encodedKey = Base64.getEncoder().encodeToString(rawKey);
        sessionKeysBySessionId.put(sessionId, encodedKey);
        return encodedKey;
    }

    public Optional<String> getSessionKey(String sessionId) {
        return Optional.ofNullable(sessionKeysBySessionId.get(sessionId));
    }

    public boolean isEncryptionEnabled() {
        return encryptionProperties.isEnabled();
    }

    @EventListener
    public void onChatSessionTerminated(ChatSessionTerminatedEvent event) {
        sessionKeysBySessionId.remove(event.sessionId());
    }
}

