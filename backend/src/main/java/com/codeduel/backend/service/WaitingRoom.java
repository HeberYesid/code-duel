package com.codeduel.backend.service;

import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe in-memory waiting room for matchmaking.
 *
 * Maintains one FIFO queue per DifficultyLevel. When a player joins,
 * if there's already someone waiting in that difficulty queue (who isn't
 * the same user), they are immediately matched and both removed from the queue.
 *
 * All operations are thread-safe via ConcurrentHashMap + synchronized matching logic.
 */
@Slf4j
@Component
public class WaitingRoom {

    /**
     * Represents a player waiting in the matchmaking queue.
     */
    public record QueueEntry(
            UUID userId,
            String username,
            String sessionId,
            DifficultyLevel difficulty,
            Instant joinedAt
    ) {}

    private final ConcurrentHashMap<DifficultyLevel, ConcurrentLinkedQueue<QueueEntry>> queues =
            new ConcurrentHashMap<>();

    /** Quick lookup: userId → which queue they're in (to enforce one-queue-at-a-time) */
    private final ConcurrentHashMap<UUID, DifficultyLevel> playerQueues =
            new ConcurrentHashMap<>();

    public WaitingRoom() {
        // Initialize one queue per difficulty
        for (DifficultyLevel level : DifficultyLevel.values()) {
            queues.put(level, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Attempts to add a player to the queue for the given difficulty.
     *
     * @return Optional containing the matched opponent if a match was found immediately,
     *         or empty if the player was added to the queue to wait.
     * @throws IllegalStateException if the player is already in a queue
     */
    public synchronized Optional<QueueEntry> addPlayer(QueueEntry entry) {
        // Guard: player can only be in one queue at a time
        if (playerQueues.containsKey(entry.userId())) {
            throw new IllegalStateException(
                    "Player " + entry.username() + " is already in a queue (" +
                    playerQueues.get(entry.userId()) + ")"
            );
        }

        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(entry.difficulty());

        // Try to find a valid opponent (not the same user)
        QueueEntry opponent = null;
        while ((opponent = queue.peek()) != null) {
            if (opponent.userId().equals(entry.userId())) {
                // Same user (shouldn't happen due to guard, but defensive)
                break;
            }
            // Found a valid opponent — remove them from the queue
            queue.poll();
            playerQueues.remove(opponent.userId());

            log.info("Match found: {} vs {} [{}]",
                    entry.username(), opponent.username(), entry.difficulty());
            return Optional.of(opponent);
        }

        // No opponent available — add to queue and wait
        queue.add(entry);
        playerQueues.put(entry.userId(), entry.difficulty());

        log.info("Player {} joined queue [{}]. Queue size: {}",
                entry.username(), entry.difficulty(), queue.size());
        return Optional.empty();
    }

    /**
     * Removes a player from whatever queue they're in.
     *
     * @return true if the player was found and removed, false otherwise
     */
    public synchronized boolean removePlayer(UUID userId) {
        DifficultyLevel difficulty = playerQueues.remove(userId);
        if (difficulty == null) {
            return false;
        }

        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(difficulty);
        boolean removed = queue.removeIf(entry -> entry.userId().equals(userId));

        if (removed) {
            log.info("Player {} removed from queue [{}]. Queue size: {}",
                    userId, difficulty, queue.size());
        }
        return removed;
    }

    /**
     * Checks if a player is currently in any queue.
     */
    public boolean isPlayerInQueue(UUID userId) {
        return playerQueues.containsKey(userId);
    }

    /**
     * Returns the current size of a difficulty queue (for debugging/monitoring).
     */
    public int getQueueSize(DifficultyLevel difficulty) {
        return queues.get(difficulty).size();
    }
}
