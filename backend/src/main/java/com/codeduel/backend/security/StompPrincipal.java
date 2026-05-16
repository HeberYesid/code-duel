package com.codeduel.backend.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Principal implementation for STOMP WebSocket sessions.
 * Carries the authenticated user's identity through the WS lifecycle.
 * Spring uses getName() to route /user/** destinations.
 */
public class StompPrincipal implements Principal {

    private final UUID userId;
    private final String username;

    public StompPrincipal(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
