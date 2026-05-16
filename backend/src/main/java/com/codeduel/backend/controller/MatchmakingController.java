package com.codeduel.backend.controller;

import com.codeduel.backend.dto.MatchmakingJoinMessage;
import com.codeduel.backend.security.StompPrincipal;
import com.codeduel.backend.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP controller for matchmaking operations.
 *
 * Client destinations:
 *   /app/matchmaking/join  → Join a queue (payload: { difficulty: "EASY" })
 *   /app/matchmaking/leave → Leave current queue (no payload)
 *
 * Server destinations (subscriptions):
 *   /user/queue/matchmaking → Match found notification
 *   /user/queue/errors      → Error messages
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    /**
     * Handles a request to join the matchmaking queue.
     * If a match is found immediately, both players are notified
     * via /user/queue/matchmaking by the MatchmakingService.
     */
    @MessageMapping("/matchmaking/join")
    @SendToUser("/queue/errors")
    public Map<String, String> joinQueue(
            @Payload MatchmakingJoinMessage message,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        StompPrincipal user = (StompPrincipal) principal;
        String sessionId = headerAccessor.getSessionId();

        log.info("Join queue request from {} for difficulty {}",
                user.getUsername(), message.getDifficulty());

        try {
            matchmakingService.joinQueue(
                    user.getUserId(),
                    user.getUsername(),
                    sessionId,
                    message.getDifficulty()
            );
            // Success: no error to send (match notification goes through service)
            return null;
        } catch (IllegalStateException e) {
            log.warn("Join queue failed for {}: {}", user.getUsername(), e.getMessage());
            return Map.of("message", e.getMessage());
        }
    }

    /**
     * Handles a request to leave the matchmaking queue.
     */
    @MessageMapping("/matchmaking/leave")
    public void leaveQueue(Principal principal) {
        StompPrincipal user = (StompPrincipal) principal;

        log.info("Leave queue request from {}", user.getUsername());
        matchmakingService.leaveQueue(user.getUserId());
    }
}
