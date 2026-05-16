package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * STOMP message payload sent by client to join the matchmaking queue.
 * Destination: /app/matchmaking/join
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingJoinMessage {
    private DifficultyLevel difficulty;
}
