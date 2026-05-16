package com.codeduel.backend.config;

import com.codeduel.backend.security.StompPrincipal;
import com.codeduel.backend.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket configuration with STOMP messaging protocol.
 *
 * Architecture:
 * - /ws → STOMP endpoint (raw WebSocket, no SockJS)
 * - /app → application-bound messages (client → server)
 * - /topic → broadcast destinations (server → multiple clients)
 * - /queue → private destinations (server → single client via /user/queue/*)
 *
 * Authentication flow:
 * 1. Client connects to ws://host/ws?token=JWT
 * 2. WebSocketAuthInterceptor validates JWT, stores userId/username in attributes
 * 3. Custom HandshakeHandler reads attributes, creates StompPrincipal
 * 4. Spring uses StompPrincipal.getName() to route /user/** messages
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for /topic (broadcast) and /queue (private)
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages FROM client TO server (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific destinations (/user/queue/*)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500"
                )
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    /**
                     * Creates a StompPrincipal from the attributes set by
                     * WebSocketAuthInterceptor during the handshake.
                     * This Principal is attached to the WebSocket session
                     * and used by Spring for /user/** message routing.
                     */
                    @Override
                    protected Principal determineUser(
                            ServerHttpRequest request,
                            WebSocketHandler wsHandler,
                            Map<String, Object> attributes
                    ) {
                        UUID userId = (UUID) attributes.get("userId");
                        String username = (String) attributes.get("username");
                        return new StompPrincipal(userId, username);
                    }
                });
    }
}
