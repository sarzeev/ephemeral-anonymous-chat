package com.example.chatbackend.websocket;

import com.example.chatbackend.config.SecurityHardeningProperties;
import com.example.chatbackend.security.IpJoinAttemptRateLimiter;
import com.example.chatbackend.security.JoinTokenService;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class AnonymousChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String TEMP_USER_ID_ATTRIBUTE = "tempUserId";
    public static final String JOIN_TOKEN_HASH_ATTRIBUTE = "joinTokenHash";

    private final JoinTokenService joinTokenService;
    private final IpJoinAttemptRateLimiter ipJoinAttemptRateLimiter;
    private final SecurityHardeningProperties securityHardeningProperties;

    public AnonymousChatHandshakeInterceptor(
            JoinTokenService joinTokenService,
            IpJoinAttemptRateLimiter ipJoinAttemptRateLimiter,
            SecurityHardeningProperties securityHardeningProperties
    ) {
        this.joinTokenService = joinTokenService;
        this.ipJoinAttemptRateLimiter = ipJoinAttemptRateLimiter;
        this.securityHardeningProperties = securityHardeningProperties;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String ipAddress = resolveIpAddress(request);
        if (!ipJoinAttemptRateLimiter.allowAttempt(
                ipAddress,
                securityHardeningProperties.getMaxJoinAttemptsPerIpPerMinute()
        )) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }

        var queryParams = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        List<String> tempUserIds = queryParams.get("tempUserId");
        List<String> joinTokens = queryParams.get("joinToken");
        if (tempUserIds == null || tempUserIds.isEmpty() || joinTokens == null || joinTokens.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        return joinTokenService.validateAndConsume(tempUserIds.get(0), joinTokens.get(0))
                .map(validated -> {
                    attributes.put(TEMP_USER_ID_ATTRIBUTE, validated.tempUserId());
                    attributes.put(JOIN_TOKEN_HASH_ATTRIBUTE, validated.joinTokenHash());
                    return true;
                })
                .orElseGet(() -> {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                });
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Placeholder for future handshake auditing/session bootstrap.
    }

    private String resolveIpAddress(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String forwardedFor = servletRequest.getServletRequest().getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }
}
