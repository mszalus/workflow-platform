package com.workflowplatform.notification.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class NotificationWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for /topic (broadcast) and /queue (point-to-point)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations (e.g., /user/{username}/queue/notifications)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();

        // Also register a raw WebSocket endpoint without SockJS fallback
        registry.addEndpoint("/ws-native")
            .setAllowedOriginPatterns("*");
    }
}
