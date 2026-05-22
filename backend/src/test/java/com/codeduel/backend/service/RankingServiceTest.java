package com.codeduel.backend.service;

import com.codeduel.backend.model.Duel;
import com.codeduel.backend.model.ScoreEntry;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.DuelFinishReason;
import com.codeduel.backend.model.enums.DuelStatus;
import com.codeduel.backend.repository.ScoreEntryRepository;
import com.codeduel.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService Unit Tests")
class RankingServiceTest {

    @Mock
    private ScoreEntryRepository scoreEntryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RankingService rankingService;

    private User winner;
    private User loser;
    private ScoreEntry winnerScore;
    private ScoreEntry loserScore;

    @BeforeEach
    void setUp() {
        winner = User.builder()
                .id(UUID.randomUUID())
                .username("winner")
                .email("winner@test.com")
                .passwordHash("hash")
                .build();
        loser = User.builder()
                .id(UUID.randomUUID())
                .username("loser")
                .email("loser@test.com")
                .passwordHash("hash")
                .build();

        winnerScore = ScoreEntry.builder()
                .id(UUID.randomUUID())
                .user(winner)
                .elo(1000)
                .wins(0)
                .losses(0)
                .draws(0)
                .duelsPlayed(0)
                .build();
        loserScore = ScoreEntry.builder()
                .id(UUID.randomUUID())
                .user(loser)
                .elo(1000)
                .wins(0)
                .losses(0)
                .draws(0)
                .duelsPlayed(0)
                .build();
    }

    @Test
    @DisplayName("Should update ELO, stats and notifications when a duel has a winner")
    void processDuelResult_WhenSolved_ShouldUpdateRatingsAndNotifyBothPlayers() {
        Duel duel = finishedDuel(DuelFinishReason.SOLVED, winner);

        when(scoreEntryRepository.findByUserId(winner.getId())).thenReturn(Optional.of(winnerScore));
        when(scoreEntryRepository.findByUserId(loser.getId())).thenReturn(Optional.of(loserScore));

        rankingService.processDuelResult(duel);

        assertThat(winnerScore.getElo()).isEqualTo(1016);
        assertThat(loserScore.getElo()).isEqualTo(984);
        assertThat(winnerScore.getWins()).isEqualTo(1);
        assertThat(winnerScore.getDuelsPlayed()).isEqualTo(1);
        assertThat(loserScore.getLosses()).isEqualTo(1);
        assertThat(loserScore.getDuelsPlayed()).isEqualTo(1);

        verify(scoreEntryRepository).save(winnerScore);
        verify(scoreEntryRepository).save(loserScore);
        verify(notificationService).createRankChangeNotification(winner, 1000, 1016);
        verify(notificationService).createRankChangeNotification(loser, 1000, 984);
    }

    @Test
    @DisplayName("Should record a draw without ELO changes or notifications on tied timeout")
    void processDuelResult_WhenTimeoutDraw_ShouldOnlyUpdateDrawStats() {
        Duel duel = finishedDuel(DuelFinishReason.TIMEOUT, null);

        when(scoreEntryRepository.findByUserId(winner.getId())).thenReturn(Optional.of(winnerScore));
        when(scoreEntryRepository.findByUserId(loser.getId())).thenReturn(Optional.of(loserScore));

        rankingService.processDuelResult(duel);

        assertThat(winnerScore.getElo()).isEqualTo(1000);
        assertThat(loserScore.getElo()).isEqualTo(1000);
        assertThat(winnerScore.getDraws()).isEqualTo(1);
        assertThat(loserScore.getDraws()).isEqualTo(1);
        assertThat(winnerScore.getDuelsPlayed()).isEqualTo(1);
        assertThat(loserScore.getDuelsPlayed()).isEqualTo(1);

        verify(scoreEntryRepository).save(winnerScore);
        verify(scoreEntryRepository).save(loserScore);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should treat forfeit as a normal win and loss")
    void processDuelResult_WhenForfeit_ShouldUpdateRatingsLikeRegularVictory() {
        Duel duel = finishedDuel(DuelFinishReason.FORFEIT, winner);

        when(scoreEntryRepository.findByUserId(winner.getId())).thenReturn(Optional.of(winnerScore));
        when(scoreEntryRepository.findByUserId(loser.getId())).thenReturn(Optional.of(loserScore));

        rankingService.processDuelResult(duel);

        assertThat(winnerScore.getElo()).isEqualTo(1016);
        assertThat(loserScore.getElo()).isEqualTo(984);
        assertThat(winnerScore.getWins()).isEqualTo(1);
        assertThat(loserScore.getLosses()).isEqualTo(1);

        verify(notificationService).createRankChangeNotification(winner, 1000, 1016);
        verify(notificationService).createRankChangeNotification(loser, 1000, 984);
    }

    @Test
    @DisplayName("Should create a default score entry when legacy users request their stats")
    void getPlayerStats_WhenLegacyUserHasNoScoreEntry_ShouldCreateDefaultEntry() {
        when(userRepository.findByUsername("winner")).thenReturn(Optional.of(winner));
        when(scoreEntryRepository.findByUserId(winner.getId())).thenReturn(Optional.empty());
        when(scoreEntryRepository.save(any(ScoreEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = rankingService.getPlayerStats("winner");

        assertThat(response.getUsername()).isEqualTo("winner");
        assertThat(response.getElo()).isEqualTo(1000);
        assertThat(response.getWins()).isEqualTo(0);
        assertThat(response.getLosses()).isEqualTo(0);
        assertThat(response.getDraws()).isEqualTo(0);
        assertThat(response.getDuelsPlayed()).isEqualTo(0);
        verify(scoreEntryRepository).save(any(ScoreEntry.class));
    }

    @Test
    @DisplayName("Should include the current user in the leaderboard even when legacy accounts have no score entry")
    void getLeaderboard_WhenLegacyCurrentUserHasNoScoreEntry_ShouldCreateDefaultEntry() {
        when(userRepository.findByUsername("winner")).thenReturn(Optional.of(winner));
        when(scoreEntryRepository.findByUserId(winner.getId())).thenReturn(Optional.empty());
        when(scoreEntryRepository.save(any(ScoreEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scoreEntryRepository.findLeaderboardOrdered()).thenAnswer(invocation -> java.util.List.of(
                ScoreEntry.builder()
                        .user(winner)
                        .elo(1000)
                        .wins(0)
                        .losses(0)
                        .draws(0)
                        .duelsPlayed(0)
                        .build(),
                loserScore
        ));

        var response = rankingService.getLeaderboard("winner", 10);

        assertThat(response.getCurrentUser()).isNotNull();
        assertThat(response.getCurrentUser().getUsername()).isEqualTo("winner");
        verify(scoreEntryRepository).save(any(ScoreEntry.class));
    }

    private Duel finishedDuel(DuelFinishReason reason, User duelWinner) {
        return Duel.builder()
                .id(UUID.randomUUID())
                .player1(winner)
                .player2(loser)
                .winner(duelWinner)
                .difficulty(DifficultyLevel.EASY)
                .status(DuelStatus.FINISHED)
                .finishReason(reason)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .finishedAt(LocalDateTime.now())
                .build();
    }
}
