package com.example.chatbackend.security;

import com.example.chatbackend.config.SecurityHardeningProperties;
import com.example.chatbackend.model.JoinCredentials;
import com.example.chatbackend.util.MessageHashingUtil;
import com.example.chatbackend.util.TemporaryIdGenerator;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class JoinTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecurityHardeningProperties securityHardeningProperties;
    private final ConcurrentMap<String, PendingJoinAuthorization> pendingJoinAuthorizations = new ConcurrentHashMap<>();

    public JoinTokenService(SecurityHardeningProperties securityHardeningProperties) {
        this.securityHardeningProperties = securityHardeningProperties;
    }

    public JoinCredentials issueJoinCredentials() {
        String tempUserId = TemporaryIdGenerator.generate();
        String joinToken = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(securityHardeningProperties.getJoinTokenTtlSeconds());

        pendingJoinAuthorizations.put(tempUserId, new PendingJoinAuthorization(
                tempUserId,
                MessageHashingUtil.sha256(joinToken),
                expiresAt
        ));

        return new JoinCredentials(tempUserId, joinToken);
    }

    public Optional<ValidatedJoinToken> validateAndConsume(String tempUserId, String joinToken) {
        PendingJoinAuthorization authorization = pendingJoinAuthorizations.remove(tempUserId);
        if (authorization == null || authorization.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        String joinTokenHash = MessageHashingUtil.sha256(joinToken);
        if (!authorization.joinTokenHash().equals(joinTokenHash)) {
            return Optional.empty();
        }

        return Optional.of(new ValidatedJoinToken(authorization.tempUserId(), authorization.joinTokenHash()));
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record PendingJoinAuthorization(
            String tempUserId,
            String joinTokenHash,
            Instant expiresAt
    ) {
    }

    public record ValidatedJoinToken(String tempUserId, String joinTokenHash) {
    }

    public String hashJoinToken(String joinToken) {
        return MessageHashingUtil.sha256(joinToken);
    }
}
