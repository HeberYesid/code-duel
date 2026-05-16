package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * STOMP message payload sent by server when a match is found.
 * Destination: /user/queue/matchmaking
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchFoundMessage {
    private String matchId;
    private String opponentUsername;
    private DifficultyLevel difficulty;
}
