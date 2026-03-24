package com.example.chatbackend.service;

import com.example.chatbackend.session.AnonymousSessionManager;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final AnonymousSessionManager anonymousSessionManager;

    public SessionService(AnonymousSessionManager anonymousSessionManager) {
        this.anonymousSessionManager = anonymousSessionManager;
    }
}

