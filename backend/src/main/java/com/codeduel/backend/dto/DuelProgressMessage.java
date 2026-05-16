package com.codeduel.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * STOMP message broadcast to the duel room with player progress.
 * Destination: /topic/duel/{duelId}/progress
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelProgressMessage {
    private String duelId;
    private String playerUsername;
    private int testsPassedCount;
    private int totalTests;
    private LocalDateTime lastActivity;
}
