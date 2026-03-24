package com.example.chatbackend.session;

import com.example.chatbackend.config.SessionProperties;
import com.example.chatbackend.model.ChatSession;
import com.example.chatbackend.model.TempUser;
import com.example.chatbackend.service.EncryptionService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AnonymousSessionManager {

    private static final Logger log = LoggerFactory.getLogger(AnonymousSessionManager.class);

    private final Object monitor = new Object();
    private final ConcurrentMap<String, TempUser> activeTempUsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ChatSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> userToSessionMap = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SessionProperties sessionProperties;
    private final EncryptionService encryptionService;

    private volatile String waitingTempUserId;

    public AnonymousSessionManager(
            ApplicationEventPublisher applicationEventPublisher,
            SessionProperties sessionProperties,
            EncryptionService encryptionService
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.sessionProperties = sessionProperties;
        this.encryptionService = encryptionService;
    }

    public void onWebSocketConnected(String websocketSessionId, String tempUserId) {
        throw new IllegalStateException("Use authorized WebSocket connection flow with join token hash");
    }

    public void onWebSocketConnected(String websocketSessionId, String tempUserId, String joinTokenHash) {
        attachWebSocketToTempUser(tempUserId, joinTokenHash, websocketSessionId);
        joinSession(tempUserId);
        log.info("WebSocket connected sessionId={} tempUserId={}", websocketSessionId, tempUserId);
    }

    public void onWebSocketDisconnected(String websocketSessionId, String tempUserId) {
        removeTempUser(tempUserId);
        log.info("WebSocket disconnected sessionId={} tempUserId={}", websocketSessionId, tempUserId);
    }

    public TempUser createTempUser(String tempUserId, String joinTokenHash, String websocketSessionId) {
        Instant now = Instant.now();
        TempUser tempUser = new TempUser(tempUserId, joinTokenHash, websocketSessionId, now, now);

        synchronized (monitor) {
            activeTempUsers.put(tempUserId, tempUser);
            if (tempUserId.equals(waitingTempUserId)) {
                waitingTempUserId = null;
            }
        }

        return tempUser;
    }

    public TempUser registerIssuedTempUser(String tempUserId, String joinTokenHash) {
        return createTempUser(tempUserId, joinTokenHash, null);
    }

    public TempUser attachWebSocketToTempUser(String tempUserId, String joinTokenHash, String websocketSessionId) {
        Instant now = Instant.now();

        synchronized (monitor) {
            TempUser existingUser = activeTempUsers.get(tempUserId);
            TempUser updatedUser;

            if (existingUser == null) {
                updatedUser = new TempUser(tempUserId, joinTokenHash, websocketSessionId, now, now);
            } else if (!existingUser.joinTokenHash().equals(joinTokenHash)) {
                throw new IllegalStateException("Join token hash mismatch for temp user");
            } else {
                updatedUser = existingUser.withWebSocketSession(websocketSessionId, now);
            }

            activeTempUsers.put(tempUserId, updatedUser);
            return updatedUser;
        }
    }

    public Optional<ChatSession> joinSession(String tempUserId) {
        ChatSession initializedSession = null;
        synchronized (monitor) {
            TempUser joiningUser = activeTempUsers.get(tempUserId);
            if (joiningUser == null) {
                return Optional.empty();
            }

            String existingSessionId = userToSessionMap.get(tempUserId);
            if (existingSessionId != null) {
                return Optional.ofNullable(activeSessions.get(existingSessionId));
            }

            String waitingUserId = waitingTempUserId;
            if (waitingUserId == null || waitingUserId.equals(tempUserId)) {
                waitingTempUserId = tempUserId;
                return Optional.empty();
            }

            TempUser waitingUser = activeTempUsers.get(waitingUserId);
            if (waitingUser == null || userToSessionMap.containsKey(waitingUserId)) {
                waitingTempUserId = tempUserId;
                return Optional.empty();
            }

            if (activeSessions.size() >= sessionProperties.getMaxActiveSessions()) {
                log.warn("Reached active session capacity maxActiveSessions={}", sessionProperties.getMaxActiveSessions());
                return Optional.empty();
            }

            Instant now = Instant.now();
            ChatSession chatSession = new ChatSession(
                    UUID.randomUUID().toString(),
                    waitingUser.withLastSeen(now),
                    joiningUser.withLastSeen(now),
                    now,
                    now
            );

            activeTempUsers.put(waitingUserId, chatSession.userA());
            activeTempUsers.put(tempUserId, chatSession.userB());
            activeSessions.put(chatSession.sessionId(), chatSession);
            userToSessionMap.put(waitingUserId, chatSession.sessionId());
            userToSessionMap.put(tempUserId, chatSession.sessionId());
            waitingTempUserId = null;
            initializedSession = chatSession;

            log.info(
                    "Created ephemeral session sessionId={} userA={} userB={}",
                    chatSession.sessionId(),
                    waitingUserId,
                    tempUserId
            );
        }

        if (initializedSession != null) {
            String sessionKey = encryptionService.initializeSessionKey(initializedSession.sessionId());
            applicationEventPublisher.publishEvent(new ChatSessionInitializedEvent(
                    initializedSession.sessionId(),
                    initializedSession.userA().tempUserId(),
                    initializedSession.userB().tempUserId(),
                    sessionKey,
                    encryptionService.isEncryptionEnabled()
            ));
            return Optional.of(initializedSession);
        }

        return Optional.empty();
    }

    public void terminateSession(String sessionId) {
        boolean terminated = false;
        synchronized (monitor) {
            ChatSession removedSession = activeSessions.remove(sessionId);
            if (removedSession == null) {
                return;
            }

            userToSessionMap.remove(removedSession.userA().tempUserId());
            userToSessionMap.remove(removedSession.userB().tempUserId());

            if (removedSession.userA().tempUserId().equals(waitingTempUserId)
                    || removedSession.userB().tempUserId().equals(waitingTempUserId)) {
                waitingTempUserId = null;
            }

            log.info("Terminated ephemeral session sessionId={}", sessionId);
            terminated = true;
        }

        if (terminated) {
            applicationEventPublisher.publishEvent(new ChatSessionTerminatedEvent(sessionId));
        }
    }

    public void removeTempUser(String tempUserId) {
        String terminatedSessionId = null;
        synchronized (monitor) {
            TempUser removedUser = activeTempUsers.remove(tempUserId);
            if (removedUser == null) {
                return;
            }

            if (tempUserId.equals(waitingTempUserId)) {
                waitingTempUserId = null;
            }

            String sessionId = userToSessionMap.get(tempUserId);
            if (sessionId != null) {
                ChatSession chatSession = activeSessions.remove(sessionId);
                if (chatSession != null) {
                    userToSessionMap.remove(chatSession.userA().tempUserId());
                    userToSessionMap.remove(chatSession.userB().tempUserId());

                    TempUser otherUser = chatSession.userA().tempUserId().equals(tempUserId)
                            ? chatSession.userB()
                            : chatSession.userA();

                    if (activeTempUsers.containsKey(otherUser.tempUserId())) {
                        waitingTempUserId = otherUser.tempUserId();
                    }

                    log.info(
                            "Removed temp user and destroyed session tempUserId={} sessionId={} peerUserId={}",
                            tempUserId,
                            sessionId,
                            otherUser.tempUserId()
                    );
                    terminatedSessionId = sessionId;
                } else {
                    userToSessionMap.remove(tempUserId);
                    terminatedSessionId = sessionId;
                }
            }
            if (terminatedSessionId == null) {
                userToSessionMap.remove(tempUserId);
                log.info("Removed temp user tempUserId={}", tempUserId);
            }
        }

        if (terminatedSessionId != null) {
            applicationEventPublisher.publishEvent(new ChatSessionTerminatedEvent(terminatedSessionId));
        }
    }

    public Optional<ChatSession> getSessionByUser(String tempUserId) {
        synchronized (monitor) {
            String sessionId = userToSessionMap.get(tempUserId);
            if (sessionId == null) {
                return Optional.empty();
            }

            return Optional.ofNullable(activeSessions.get(sessionId));
        }
    }

    public Optional<ChatSession> getSession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    public Optional<TempUser> getTempUser(String tempUserId) {
        return Optional.ofNullable(activeTempUsers.get(tempUserId));
    }

    public Map<String, TempUser> getActiveTempUsersSnapshot() {
        return Map.copyOf(activeTempUsers);
    }

    public Map<String, ChatSession> getActiveSessionsSnapshot() {
        return Map.copyOf(activeSessions);
    }

    public Map<String, String> getUserToSessionMapSnapshot() {
        return Map.copyOf(userToSessionMap);
    }

    public void cleanupExpiredSessions() {
        log.debug("Session cleanup placeholder invoked");
    }
}
