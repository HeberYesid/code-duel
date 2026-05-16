package com.codeduel.backend.service;


import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Orchestrates the matchmaking flow:
 * 1. Players join a difficulty queue via STOMP
 * 2. When two players are in the same queue → immediate match
 * 3. Each player receives the opponent's username via /user/queue/matchmaking
 *
 * Disconnect handling:
 * - When a WebSocket disconnects, a 60s grace period starts
 * - If the user reconnects within 60s, the pending removal is cancelled
 * - If not, the user is removed from whatever queue they were in
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final WaitingRoom waitingRoom;
    private final SimpMessagingTemplate messagingTemplate;
    private final DuelService duelService;

    /** Maps sessionId → userId for disconnect event lookups */
    private final ConcurrentHashMap<String, UUID> sessionUserMap = new ConcurrentHashMap<>();

    /** Maps sessionId → username (needed for reconnect scenarios) */
    private final ConcurrentHashMap<String, String> sessionUsernameMap = new ConcurrentHashMap<>();

    /** Maps userId → pending disconnect removal (to cancel on reconnect) */
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingRemovals = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "matchmaking-disconnect-scheduler");
        t.setDaemon(true);
        return t;
    });

    private static final long DISCONNECT_GRACE_PERIOD_SECONDS = 60;

    /**
     * Registers a WebSocket session for tracking.
     * Called when a STOMP session connects (SessionConnectEvent).
     */
    public void registerSession(String sessionId, UUID userId, String username) {
        sessionUserMap.put(sessionId, userId);
        sessionUsernameMap.put(sessionId, username);

        // Cancel any pending removal from a previous disconnect
        ScheduledFuture<?> pendingRemoval = pendingRemovals.remove(userId);
        if (pendingRemoval != null) {
            pendingRemoval.cancel(false);
            log.info("Reconnect detected for user {}. Cancelled pending queue removal.", username);
        }

        log.debug("Session registered: {} → {}", sessionId, username);
    }

    /**
     * Attempts to join the matchmaking queue for a given difficulty.
     *
     * @throws IllegalStateException if the player is already in a queue
     */
    public void joinQueue(UUID userId, String username, String sessionId, DifficultyLevel difficulty) {
        WaitingRoom.QueueEntry entry = new WaitingRoom.QueueEntry(
                userId, username, sessionId, difficulty, Instant.now()
        );

        Optional<WaitingRoom.QueueEntry> opponent = waitingRoom.addPlayer(entry);

        if (opponent.isPresent()) {
            notifyMatch(entry, opponent.get(), difficulty);
        }
        // If no opponent, player is queued — frontend already shows the searching UI
    }

    /**
     * Removes the player from whatever queue they're in.
     */
    public void leaveQueue(UUID userId) {
        boolean removed = waitingRoom.removePlayer(userId);
        if (removed) {
            log.info("Player {} left the queue voluntarily", userId);
        }
    }

    /**
     * Handles WebSocket disconnect by scheduling a delayed queue removal.
     * If the user reconnects within the grace period, the removal is cancelled.
     */
    public void handleDisconnect(String sessionId) {
        UUID userId = sessionUserMap.remove(sessionId);
        String username = sessionUsernameMap.remove(sessionId);

        if (userId == null) {
            return; // Unknown session, nothing to do
        }

        // Only schedule removal if the player is actually in a queue
        if (!waitingRoom.isPlayerInQueue(userId)) {
            log.debug("Disconnected user {} was not in any queue", username);
            return;
        }

        log.info("User {} disconnected. Scheduling queue removal in {}s",
                username, DISCONNECT_GRACE_PERIOD_SECONDS);

        ScheduledFuture<?> removal = scheduler.schedule(() -> {
            boolean removed = waitingRoom.removePlayer(userId);
            pendingRemovals.remove(userId);
            if (removed) {
                log.info("User {} removed from queue after {}s disconnect timeout",
                        username, DISCONNECT_GRACE_PERIOD_SECONDS);
            }
        }, DISCONNECT_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);

        pendingRemovals.put(userId, removal);
    }

    /**
     * Delegates duel creation to DuelService.
     * DuelService handles: challenge selection, DB persistence, STOMP notifications.
     */
    private void notifyMatch(
            WaitingRoom.QueueEntry player1,
            WaitingRoom.QueueEntry player2,
            DifficultyLevel difficulty
    ) {
        log.info("Match found: {} vs {} at {} difficulty. Creating duel...",
                player1.username(), player2.username(), difficulty);

        try {
            UUID duelId = duelService.createDuel(
                    player1.userId(), player1.username(),
                    player2.userId(), player2.username(),
                    difficulty
            );
            log.info("Duel [{}] created and players notified.", duelId);
        } catch (Exception e) {
            log.error("Failed to create duel for {} vs {}: {}",
                    player1.username(), player2.username(), e.getMessage(), e);
            // Notify both players of the error
            messagingTemplate.convertAndSendToUser(
                    player1.username(), "/queue/errors",
                    java.util.Map.of("message", "Failed to create duel. Please try again."));
            messagingTemplate.convertAndSendToUser(
                    player2.username(), "/queue/errors",
                    java.util.Map.of("message", "Failed to create duel. Please try again."));
        }
    }
}
