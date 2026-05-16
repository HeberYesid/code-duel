package com.codeduel.backend.controller;

import com.codeduel.backend.dto.DuelSubmitMessage;
import com.codeduel.backend.security.StompPrincipal;
import com.codeduel.backend.service.DuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP controller for duel operations during an active match.
 *
 * Client destinations:
 *   /app/duel/submit → Submit code during a duel (payload: { duelId, code })
 *
 * Server destinations (subscriptions):
 *   /user/queue/duel/result          → Private submission result
 *   /topic/duel/{duelId}/progress    → Public progress updates
 *   /topic/duel/{duelId}/finished    → Duel end notification
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DuelController {

    private final DuelService duelService;

    /**
     * Handles code submission during an active duel.
     * Code execution happens asynchronously — results are pushed via STOMP.
     */
    @MessageMapping("/duel/submit")
    @SendToUser("/queue/errors")
    public Map<String, String> handleSubmission(
            @Payload DuelSubmitMessage message,
            Principal principal
    ) {
        StompPrincipal user = (StompPrincipal) principal;

        log.info("Duel submission from {} for duel {}", user.getUsername(), message.getDuelId());

        try {
            UUID duelId = UUID.fromString(message.getDuelId());
            duelService.submitCode(
                    duelId,
                    user.getUserId(),
                    user.getUsername(),
                    message.getCode()
            );
            // Success: results will arrive asynchronously via /user/queue/duel/result
            return null;
        } catch (Exception e) {
            log.error("Duel submission error for {}: {}", user.getUsername(), e.getMessage());
            return Map.of("message", "Submission failed: " + e.getMessage());
        }
    }
}
