package com.example.chatbackend.config;

import com.example.chatbackend.websocket.AnonymousChatHandshakeInterceptor;
import com.example.chatbackend.websocket.TempUserHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AnonymousChatHandshakeInterceptor handshakeInterceptor;
    private final TempUserHandshakeHandler handshakeHandler;

    public WebSocketConfig(
            AnonymousChatHandshakeInterceptor handshakeInterceptor,
            TempUserHandshakeHandler handshakeHandler
    ) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
