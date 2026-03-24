package com.example.chatbackend.scheduler;

import com.example.chatbackend.session.AnonymousSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);

    private final AnonymousSessionManager anonymousSessionManager;

    public SessionCleanupScheduler(AnonymousSessionManager anonymousSessionManager) {
        this.anonymousSessionManager = anonymousSessionManager;
    }

    @Scheduled(fixedDelayString = "${app.session.cleanup-interval-ms:30000}")
    public void cleanupExpiredSessions() {
        log.debug("Running scheduled session cleanup");
        anonymousSessionManager.cleanupExpiredSessions();
    }
}

