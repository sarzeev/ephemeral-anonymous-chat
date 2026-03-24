package com.example.chatbackend.controller;

import com.example.chatbackend.config.SecurityHardeningProperties;
import com.example.chatbackend.model.JoinCredentials;
import com.example.chatbackend.security.IpJoinAttemptRateLimiter;
import com.example.chatbackend.security.JoinTokenService;
import com.example.chatbackend.session.AnonymousSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionBootstrapController {

    private final JoinTokenService joinTokenService;
    private final AnonymousSessionManager anonymousSessionManager;
    private final IpJoinAttemptRateLimiter ipJoinAttemptRateLimiter;
    private final SecurityHardeningProperties securityHardeningProperties;

    public SessionBootstrapController(
            JoinTokenService joinTokenService,
            AnonymousSessionManager anonymousSessionManager,
            IpJoinAttemptRateLimiter ipJoinAttemptRateLimiter,
            SecurityHardeningProperties securityHardeningProperties
    ) {
        this.joinTokenService = joinTokenService;
        this.anonymousSessionManager = anonymousSessionManager;
        this.ipJoinAttemptRateLimiter = ipJoinAttemptRateLimiter;
        this.securityHardeningProperties = securityHardeningProperties;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<JoinCredentials> bootstrapSession(HttpServletRequest request) {
        String ipAddress = resolveIpAddress(request);
        if (!ipJoinAttemptRateLimiter.allowAttempt(
                ipAddress,
                securityHardeningProperties.getMaxJoinAttemptsPerIpPerMinute()
        )) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        JoinCredentials credentials = joinTokenService.issueJoinCredentials();
        anonymousSessionManager.registerIssuedTempUser(
                credentials.tempUserId(),
                joinTokenService.hashJoinToken(credentials.joinToken())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(credentials);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
