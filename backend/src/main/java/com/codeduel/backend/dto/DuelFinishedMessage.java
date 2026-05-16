package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DuelFinishReason;
import lombok.*;

/**
 * STOMP message broadcast when a duel ends.
 * Destination: /topic/duel/{duelId}/finished
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelFinishedMessage {
    private String duelId;
    /** Null if draw */
    private String winnerUsername;
    private DuelFinishReason finishReason;
    private int player1TestsPassed;
    private int player2TestsPassed;
    private String player1Username;
    private String player2Username;
}
