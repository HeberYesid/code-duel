package com.codeduel.backend.config;

import com.codeduel.backend.security.StompPrincipal;
import com.codeduel.backend.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${cors.ws-allowed-origins:http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500}")
    private String wsAllowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = wsAllowedOrigins != null && !wsAllowedOrigins.isBlank()
                ? Arrays.asList(wsAllowedOrigins.split(",")).toArray(new String[0])
                : new String[0];
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.length > 0 ? origins : new String[]{"*"})
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
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
