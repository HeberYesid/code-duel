package com.codeduel.backend.dto;

import lombok.*;

/**
 * STOMP message received from client when submitting code during a duel.
 * Destination: /app/duel/submit
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelSubmitMessage {
    private String duelId;
    private String code;
}
