package com.example.chatbackend.scheduler;

import com.example.chatbackend.service.MessageLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MessageExpiryScheduler.class);

    private final MessageLifecycleManager messageLifecycleManager;

    public MessageExpiryScheduler(MessageLifecycleManager messageLifecycleManager) {
        this.messageLifecycleManager = messageLifecycleManager;
    }

    @Scheduled(fixedDelayString = "${app.message.expiry-scan-interval-ms:1000}")
    public void removeExpiredMessages() {
        log.debug("Running scheduled message expiry scan");
        messageLifecycleManager.removeExpiredMessages();
    }
}

