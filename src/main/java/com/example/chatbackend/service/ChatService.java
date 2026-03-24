package com.example.chatbackend.service;

import com.example.chatbackend.config.MessagingProperties;
import com.example.chatbackend.model.ChatSession;
import com.example.chatbackend.model.Message;
import com.example.chatbackend.security.MessageHmacValidator;
import com.example.chatbackend.security.SessionReplayProtectionManager;
import com.example.chatbackend.session.AnonymousSessionManager;
import com.example.chatbackend.session.WebSocketSessionTracker;
import com.example.chatbackend.websocket.dto.ChatMessageInboundPayload;
import com.example.chatbackend.websocket.dto.ChatMessageOutboundPayload;
import com.example.chatbackend.websocket.dto.TypingEventInboundPayload;
import com.example.chatbackend.websocket.dto.TypingEventOutboundPayload;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AnonymousSessionManager anonymousSessionManager;
    private final WebSocketSessionTracker webSocketSessionTracker;
    private final MessageLifecycleManager messageLifecycleManager;
    private final SessionMessageRateLimiter sessionMessageRateLimiter;
    private final TypingEventDebounceManager typingEventDebounceManager;
    private final MessagingProperties messagingProperties;
    private final SessionReplayProtectionManager sessionReplayProtectionManager;
    private final MessageHmacValidator messageHmacValidator;
    private final EncryptionService encryptionService;

    public ChatService(
            AnonymousSessionManager anonymousSessionManager,
            WebSocketSessionTracker webSocketSessionTracker,
            MessageLifecycleManager messageLifecycleManager,
            SessionMessageRateLimiter sessionMessageRateLimiter,
            TypingEventDebounceManager typingEventDebounceManager,
            MessagingProperties messagingProperties,
            SessionReplayProtectionManager sessionReplayProtectionManager,
            MessageHmacValidator messageHmacValidator,
            EncryptionService encryptionService
    ) {
        this.anonymousSessionManager = anonymousSessionManager;
        this.webSocketSessionTracker = webSocketSessionTracker;
        this.messageLifecycleManager = messageLifecycleManager;
        this.sessionMessageRateLimiter = sessionMessageRateLimiter;
        this.typingEventDebounceManager = typingEventDebounceManager;
        this.messagingProperties = messagingProperties;
        this.sessionReplayProtectionManager = sessionReplayProtectionManager;
        this.messageHmacValidator = messageHmacValidator;
        this.encryptionService = encryptionService;
    }

    public Optional<ChatMessageRoutingResult> routeIncomingMessage(
            String webSocketSessionId,
            ChatMessageInboundPayload payload
    ) {
        String tempUserId = resolveTempUserId(webSocketSessionId);
        ChatSession chatSession = resolveActiveSession(tempUserId);
        if (!sessionMessageRateLimiter.allowMessage(
                chatSession.sessionId(),
                messagingProperties.getMaxMessagesPerSessionPerSecond()
        )) {
            log.debug("Dropped message due to session rate limit sessionId={}", chatSession.sessionId());
            return Optional.empty();
        }

        if (!sessionReplayProtectionManager.registerMessageId(chatSession.sessionId(), payload.messageId())) {
            log.debug("Rejected replayed messageId={} sessionId={}", payload.messageId(), chatSession.sessionId());
            return Optional.empty();
        }

        String sessionKey = encryptionService.getSessionKey(chatSession.sessionId())
                .orElse(null);
        if (sessionKey == null && encryptionService.isEncryptionEnabled()) {
            sessionReplayProtectionManager.forgetMessageId(chatSession.sessionId(), payload.messageId());
            log.debug("Dropped message because session key is unavailable sessionId={}", chatSession.sessionId());
            return Optional.empty();
        }

        Instant createdAt = Instant.now();
        Instant expiryTime = createdAt.plusSeconds(10);
        Message message = new Message(
                payload.messageId(),
                chatSession.sessionId(),
                tempUserId,
                payload.encryptedPayload(),
                payload.iv(),
                createdAt,
                expiryTime,
                payload.hmacSignature()
        );

        if (encryptionService.isEncryptionEnabled() && !messageHmacValidator.isValid(sessionKey, message)) {
            sessionReplayProtectionManager.forgetMessageId(chatSession.sessionId(), payload.messageId());
            log.warn("Rejected encrypted message with invalid HMAC sessionId={} senderId={}",
                    chatSession.sessionId(),
                    tempUserId);
            return Optional.empty();
        }

        Message storedMessage = messageLifecycleManager.addMessage(chatSession.sessionId(), message)
                .orElse(null);
        if (storedMessage == null) {
            sessionReplayProtectionManager.forgetMessageId(chatSession.sessionId(), payload.messageId());
            log.debug("Dropped message for inactive sessionId={}", chatSession.sessionId());
            return Optional.empty();
        }

        log.debug("Routed inbound message sessionId={} senderId={}", chatSession.sessionId(), tempUserId);
        return Optional.of(new ChatMessageRoutingResult(
                chatSession.sessionId(),
                new ChatMessageOutboundPayload(
                        storedMessage.messageId(),
                        storedMessage.senderId(),
                        storedMessage.encryptedPayload(),
                        storedMessage.iv(),
                        storedMessage.createdAt(),
                        storedMessage.expiryTime(),
                        storedMessage.hmacSignature()
                )
        ));
    }

    public Optional<TypingEventRoutingResult> routeTypingEvent(
            String webSocketSessionId,
            TypingEventInboundPayload payload
    ) {
        String tempUserId = resolveTempUserId(webSocketSessionId);
        ChatSession chatSession = resolveActiveSession(tempUserId);
        if (!typingEventDebounceManager.shouldPublish(
                chatSession.sessionId(),
                tempUserId,
                messagingProperties.getTypingDebounceMs()
        )) {
            log.debug("Debounced typing event sessionId={} senderId={}", chatSession.sessionId(), tempUserId);
            return Optional.empty();
        }

        Instant createdAt = Instant.now();

        log.debug("Routed typing event sessionId={} senderId={}", chatSession.sessionId(), tempUserId);
        return Optional.of(new TypingEventRoutingResult(
                chatSession.sessionId(),
                new TypingEventOutboundPayload(tempUserId, payload.typing(), createdAt)
        ));
    }

    private String resolveTempUserId(String webSocketSessionId) {
        return webSocketSessionTracker.findTempUserId(webSocketSessionId)
                .orElseThrow(() -> new IllegalStateException("No temp user is associated with the WebSocket session"));
    }

    private ChatSession resolveActiveSession(String tempUserId) {
        Optional<ChatSession> chatSession = anonymousSessionManager.getSessionByUser(tempUserId);
        return chatSession.orElseThrow(() -> new IllegalStateException("No active chat session for temp user"));
    }
}
