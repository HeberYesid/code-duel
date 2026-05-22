package com.codeduel.backend.service;

import com.codeduel.backend.dto.LeaderboardEntryResponse;
import com.codeduel.backend.dto.LeaderboardResponse;
import com.codeduel.backend.dto.PlayerStatsResponse;
import com.codeduel.backend.model.Duel;
import com.codeduel.backend.model.ScoreEntry;
import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.UserRepository;
import com.codeduel.backend.repository.ScoreEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int K_FACTOR = 32;

    private final ScoreEntryRepository scoreEntryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void processDuelResult(Duel duel) {
        ScoreEntry player1Score = getOrCreateScoreEntry(duel.getPlayer1());
        ScoreEntry player2Score = getOrCreateScoreEntry(duel.getPlayer2());

        if (duel.getWinner() == null) {
            registerDraw(player1Score, player2Score);
            return;
        }

        boolean winnerIsPlayer1 = duel.getWinner().getId().equals(duel.getPlayer1().getId());
        ScoreEntry winnerScore = winnerIsPlayer1 ? player1Score : player2Score;
        ScoreEntry loserScore = winnerIsPlayer1 ? player2Score : player1Score;

        int oldWinnerElo = winnerScore.getElo();
        int oldLoserElo = loserScore.getElo();

        winnerScore.setWins(winnerScore.getWins() + 1);
        loserScore.setLosses(loserScore.getLosses() + 1);
        winnerScore.setDuelsPlayed(winnerScore.getDuelsPlayed() + 1);
        loserScore.setDuelsPlayed(loserScore.getDuelsPlayed() + 1);

        winnerScore.setElo(calculateNewElo(oldWinnerElo, oldLoserElo, 1.0));
        loserScore.setElo(calculateNewElo(oldLoserElo, oldWinnerElo, 0.0));

        scoreEntryRepository.save(winnerScore);
        scoreEntryRepository.save(loserScore);

        if (oldWinnerElo != winnerScore.getElo()) {
            notificationService.createRankChangeNotification(winnerScore.getUser(), oldWinnerElo, winnerScore.getElo());
        }
        if (oldLoserElo != loserScore.getElo()) {
            notificationService.createRankChangeNotification(loserScore.getUser(), oldLoserElo, loserScore.getElo());
        }
    }

    private void registerDraw(ScoreEntry player1Score, ScoreEntry player2Score) {
        player1Score.setDraws(player1Score.getDraws() + 1);
        player2Score.setDraws(player2Score.getDraws() + 1);
        player1Score.setDuelsPlayed(player1Score.getDuelsPlayed() + 1);
        player2Score.setDuelsPlayed(player2Score.getDuelsPlayed() + 1);

        scoreEntryRepository.save(player1Score);
        scoreEntryRepository.save(player2Score);
    }

    private ScoreEntry getScoreEntry(UUID userId) {
        return scoreEntryRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Score entry not found for user " + userId));
    }

    private ScoreEntry getOrCreateScoreEntry(User user) {
        return scoreEntryRepository.findByUserId(user.getId())
                .orElseGet(() -> scoreEntryRepository.save(ScoreEntry.builder()
                        .user(user)
                        .build()));
    }

    @Transactional
    public PlayerStatsResponse getPlayerStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
        ScoreEntry scoreEntry = getOrCreateScoreEntry(user);

        return PlayerStatsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .elo(scoreEntry.getElo())
                .wins(scoreEntry.getWins())
                .losses(scoreEntry.getLosses())
                .draws(scoreEntry.getDraws())
                .duelsPlayed(scoreEntry.getDuelsPlayed())
                .build();
    }

    @Transactional
    public LeaderboardResponse getLeaderboard(String currentUsername, int limit) {
        User currentUserEntity = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("User not found: " + currentUsername));
        getOrCreateScoreEntry(currentUserEntity);

        List<ScoreEntry> orderedEntries = scoreEntryRepository.findLeaderboardOrdered();
        List<LeaderboardEntryResponse> mappedEntries = mapLeaderboardEntries(orderedEntries);

        LeaderboardEntryResponse currentUser = mappedEntries.stream()
                .filter(entry -> entry.getUsername().equals(currentUsername))
                .findFirst()
                .orElse(null);

        return LeaderboardResponse.builder()
                .limit(limit)
                .entries(mappedEntries.stream().limit(limit).toList())
                .currentUser(currentUser)
                .build();
    }

    private int calculateNewElo(int currentElo, int opponentElo, double actualScore) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentElo - currentElo) / 400.0));
        return (int) Math.round(currentElo + K_FACTOR * (actualScore - expectedScore));
    }

    private List<LeaderboardEntryResponse> mapLeaderboardEntries(List<ScoreEntry> orderedEntries) {
        return java.util.stream.IntStream.range(0, orderedEntries.size())
                .mapToObj(index -> {
                    ScoreEntry entry = orderedEntries.get(index);
                    return LeaderboardEntryResponse.builder()
                            .rank(index + 1)
                            .userId(entry.getUser().getId())
                            .username(entry.getUser().getUsername())
                            .elo(entry.getElo())
                            .wins(entry.getWins())
                            .losses(entry.getLosses())
                            .draws(entry.getDraws())
                            .duelsPlayed(entry.getDuelsPlayed())
                            .build();
                })
                .toList();
    }
}
