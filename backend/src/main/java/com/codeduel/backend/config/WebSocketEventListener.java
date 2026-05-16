package com.codeduel.backend.config;

import com.codeduel.backend.security.StompPrincipal;
import com.codeduel.backend.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Listens for WebSocket session lifecycle events (connect/disconnect).
 *
 * On connect:  Registers the session→user mapping in MatchmakingService
 * On disconnect: Triggers the 60-second grace period for queue removal
 *
 * The grace period allows temporary network drops without losing queue position.
 * If the user reconnects within 60s, the pending removal is cancelled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final MatchmakingService matchmakingService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();

        if (principal instanceof StompPrincipal user) {
            String sessionId = accessor.getSessionId();
            matchmakingService.registerSession(sessionId, user.getUserId(), user.getUsername());
            log.info("WebSocket connected: {} (session: {})", user.getUsername(), sessionId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("WebSocket disconnected (session: {})", sessionId);
        matchmakingService.handleDisconnect(sessionId);
    }
}
