package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DifficultyLevel;
import lombok.*;

/**
 * STOMP message sent to both players when a duel starts.
 * Replaces the old MatchFoundMessage — now includes challenge info.
 * Destination: /user/queue/matchmaking
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelStartMessage {
    private String duelId;
    private String opponentUsername;
    private DifficultyLevel difficulty;
    private String challengeTitle;
    private String challengeDescription;
    private int testCaseCount;
    private int timeoutMinutes;
}
