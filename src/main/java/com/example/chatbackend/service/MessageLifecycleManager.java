package com.example.chatbackend.service;

import com.example.chatbackend.model.Message;
import com.example.chatbackend.session.AnonymousSessionManager;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(MessageLifecycleManager.class);
    private static final Duration DEFAULT_MESSAGE_TTL = Duration.ofSeconds(10);

    private final AnonymousSessionManager anonymousSessionManager;
    private final ConcurrentMap<String, Queue<Message>> sessionMessages = new ConcurrentHashMap<>();
    private final DelayQueue<ExpiringMessageReference> expiryQueue = new DelayQueue<>();

    public MessageLifecycleManager(AnonymousSessionManager anonymousSessionManager) {
        this.anonymousSessionManager = anonymousSessionManager;
    }

    public Optional<Message> addMessage(String sessionId, Message message) {
        if (anonymousSessionManager.getSession(sessionId).isEmpty()) {
            log.debug("Skipping message tracking for inactive sessionId={}", sessionId);
            return Optional.empty();
        }

        Message normalizedMessage = normalizeMessage(sessionId, message);
        Queue<Message> queue = sessionMessages.computeIfAbsent(sessionId, ignored -> new ConcurrentLinkedDeque<>());
        queue.add(normalizedMessage);

        if (anonymousSessionManager.getSession(sessionId).isEmpty()) {
            queue.remove(normalizedMessage);
            if (queue.isEmpty()) {
                sessionMessages.remove(sessionId, queue);
            }
            log.debug("Dropped message during mid-flight session termination sessionId={}", sessionId);
            return Optional.empty();
        }

        expiryQueue.offer(new ExpiringMessageReference(normalizedMessage));

        log.debug("Tracked ephemeral message messageId={} sessionId={}", normalizedMessage.messageId(), sessionId);
        return Optional.of(normalizedMessage);
    }

    public List<Message> getMessages(String sessionId) {
        Queue<Message> queue = sessionMessages.get(sessionId);
        if (queue == null) {
            return List.of();
        }

        return List.copyOf(new ArrayList<>(queue));
    }

    public void removeExpiredMessages() {
        ExpiringMessageReference expiringMessage;
        while ((expiringMessage = expiryQueue.poll()) != null) {
            Queue<Message> sessionQueue = sessionMessages.get(expiringMessage.sessionId());
            if (sessionQueue == null) {
                continue;
            }

            sessionQueue.remove(expiringMessage.message());
            if (sessionQueue.isEmpty()) {
                sessionMessages.remove(expiringMessage.sessionId(), sessionQueue);
            }
        }
    }

    public void purgeSessionMessages(String sessionId) {
        Queue<Message> removedQueue = sessionMessages.remove(sessionId);
        if (removedQueue == null || removedQueue.isEmpty()) {
            return;
        }

        expiryQueue.removeIf(reference -> Objects.equals(reference.sessionId(), sessionId));

        log.debug("Purged ephemeral messages for sessionId={}", sessionId);
    }

    private Message normalizeMessage(String sessionId, Message message) {
        Instant createdAt = message.createdAt() != null ? message.createdAt() : Instant.now();
        Instant expiryTime = message.expiryTime() != null ? message.expiryTime() : createdAt.plus(DEFAULT_MESSAGE_TTL);
        String messageId = message.messageId() != null ? message.messageId() : UUID.randomUUID().toString();

        return new Message(
                messageId,
                sessionId,
                message.senderId(),
                message.encryptedPayload(),
                message.iv(),
                createdAt,
                expiryTime,
                message.hmacSignature()
        );
    }

    private record ExpiringMessageReference(Message message) implements Delayed {

        private String sessionId() {
            return message.sessionId();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delayMillis = Duration.between(Instant.now(), message.expiryTime()).toMillis();
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }

            long thisDelay = getDelay(TimeUnit.MILLISECONDS);
            long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(thisDelay, otherDelay);
        }
    }
}
