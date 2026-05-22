package com.codeduel.backend.service;

import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.Duel;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.DuelFinishReason;
import com.codeduel.backend.model.enums.DuelStatus;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.DuelRepository;
import com.codeduel.backend.repository.SubmissionRepository;
import com.codeduel.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuelService Unit Tests")
class DuelServiceTest {

    @Mock
    private DuelRepository duelRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CodeExecutionService codeExecutionService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private RankingService rankingService;

    @InjectMocks
    private DuelService duelService;

    private User player1;
    private User player2;
    private Duel duel;

    @BeforeEach
    void setUp() {
        player1 = User.builder()
                .id(UUID.randomUUID())
                .username("player1")
                .email("player1@test.com")
                .passwordHash("hash")
                .build();
        player2 = User.builder()
                .id(UUID.randomUUID())
                .username("player2")
                .email("player2@test.com")
                .passwordHash("hash")
                .build();

        Challenge challenge = Challenge.builder()
                .id(UUID.randomUUID())
                .title("FizzBuzz")
                .description("desc")
                .difficulty(DifficultyLevel.EASY)
                .testCases(Collections.emptyList())
                .build();

        duel = Duel.builder()
                .id(UUID.randomUUID())
                .player1(player1)
                .player2(player2)
                .challenge(challenge)
                .difficulty(DifficultyLevel.EASY)
                .status(DuelStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("finishDuel should delegate the final result to RankingService")
    void finishDuel_ShouldProcessRankingWhenDuelEnds() {
        when(duelRepository.findById(duel.getId())).thenReturn(Optional.of(duel));
        when(userRepository.findById(player1.getId())).thenReturn(Optional.of(player1));
        when(submissionRepository.findByDuelIdAndUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(Collections.emptyList());

        duelService.finishDuel(duel.getId(), DuelFinishReason.SOLVED, player1.getId());

        verify(rankingService).processDuelResult(duel);
    }

    @Test
    @DisplayName("finishDuel should ignore duels that are no longer active")
    void finishDuel_ShouldNotProcessRankingForFinishedDuels() {
        duel.setStatus(DuelStatus.FINISHED);
        when(duelRepository.findById(duel.getId())).thenReturn(Optional.of(duel));

        duelService.finishDuel(duel.getId(), DuelFinishReason.SOLVED, player1.getId());

        verifyNoInteractions(rankingService);
    }
}
