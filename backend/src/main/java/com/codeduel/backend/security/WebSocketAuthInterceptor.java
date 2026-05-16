package com.codeduel.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Intercepts the HTTP→WebSocket upgrade handshake to validate the JWT token
 * provided as a query parameter (?token=xxx).
 *
 * If valid, stores userId and username in the session attributes so the
 * custom HandshakeHandler can build a StompPrincipal.
 * If invalid, rejects the handshake entirely (returns false).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token == null || token.isBlank()) {
                log.warn("WebSocket handshake rejected: no token provided");
                return false;
            }

            try {
                String username = jwtService.extractUsername(token);
                UUID userId = jwtService.extractUserId(token);

                attributes.put("userId", userId);
                attributes.put("username", username);

                log.info("WebSocket handshake authorized for user: {}", username);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket handshake rejected: invalid token — {}", e.getMessage());
                return false;
            }
        }

        log.warn("WebSocket handshake rejected: not a servlet request");
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No post-handshake processing needed
    }
}
