package com.codeduel.backend.service;

import com.codeduel.backend.dto.*;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.Duel;
import com.codeduel.backend.model.Submission;
import com.codeduel.backend.model.TestCase;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.*;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.DuelRepository;
import com.codeduel.backend.repository.SubmissionRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Orchestrates the duel lifecycle:
 * 1. Create duel on match → select challenge → notify players
 * 2. Receive code submissions → execute async → broadcast progress
 * 3. Detect winner (all tests ACCEPTED) or handle timeout/forfeit
 *
 * Thread safety: uses ConcurrentHashMaps for active timers and grace periods.
 * Code execution is async (CompletableFuture) to avoid blocking STOMP threads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuelService {

    private final DuelRepository duelRepository;
    private final ChallengeRepository challengeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CodeExecutionService codeExecutionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final int DUEL_TIMEOUT_MINUTES = 20;
    private static final int DISCONNECT_GRACE_SECONDS = 30;

    /** Active duel timeout timers: duelId → ScheduledFuture */
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> duelTimers = new ConcurrentHashMap<>();

    /** Pending forfeit timers: userId → ScheduledFuture */
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingForfeits = new ConcurrentHashMap<>();

    /** Maps userId → active duelId for quick lookup on disconnect */
    private final ConcurrentHashMap<UUID, UUID> activeDuelMap = new ConcurrentHashMap<>();

    /** Thread pool for async code execution (not STOMP threads) */
    private final ExecutorService executionPool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "duel-exec-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    /** Scheduler for timeout and forfeit timers */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "duel-scheduler-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    // ──────────────────────────────────────────────
    // 1. DUEL CREATION (called by MatchmakingService)
    // ──────────────────────────────────────────────

    @Transactional
    public UUID createDuel(UUID player1Id, String player1Username,
                           UUID player2Id, String player2Username,
                           DifficultyLevel difficulty) {

        User player1 = userRepository.findById(player1Id)
                .orElseThrow(() -> new ResourceNotFoundException("Player 1 not found"));
        User player2 = userRepository.findById(player2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Player 2 not found"));

        Challenge challenge = challengeRepository
                .findRandomByDifficulty(difficulty.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No challenges found for difficulty: " + difficulty));

        Duel duel = Duel.builder()
                .player1(player1)
                .player2(player2)
                .challenge(challenge)
                .difficulty(difficulty)
                .status(DuelStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();
        duel = duelRepository.save(duel);

        UUID duelId = duel.getId();
        log.info("Duel created [{}]: {} vs {} on '{}' ({})",
                duelId, player1Username, player2Username, challenge.getTitle(), difficulty);

        activeDuelMap.put(player1Id, duelId);
        activeDuelMap.put(player2Id, duelId);

        scheduleTimeout(duelId);

        int testCount = challenge.getTestCases().size();

        DuelStartMessage msgForPlayer1 = DuelStartMessage.builder()
                .duelId(duelId.toString())
                .opponentUsername(player2Username)
                .difficulty(difficulty)
                .challengeTitle(challenge.getTitle())
                .challengeDescription(challenge.getDescription())
                .testCaseCount(testCount)
                .timeoutMinutes(DUEL_TIMEOUT_MINUTES)
                .build();

        DuelStartMessage msgForPlayer2 = DuelStartMessage.builder()
                .duelId(duelId.toString())
                .opponentUsername(player1Username)
                .difficulty(difficulty)
                .challengeTitle(challenge.getTitle())
                .challengeDescription(challenge.getDescription())
                .testCaseCount(testCount)
                .timeoutMinutes(DUEL_TIMEOUT_MINUTES)
                .build();

        messagingTemplate.convertAndSendToUser(player1Username, "/queue/matchmaking", msgForPlayer1);
        messagingTemplate.convertAndSendToUser(player2Username, "/queue/matchmaking", msgForPlayer2);

        return duelId;
    }

    // ──────────────────────────────────────────────
    // 2. CODE SUBMISSION (called by DuelController)
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public void submitCode(UUID duelId, UUID userId, String username, String code) {
        Duel duel = duelRepository.findById(duelId)
                .orElseThrow(() -> new ResourceNotFoundException("Duel not found: " + duelId));

        if (duel.getStatus() != DuelStatus.ACTIVE) {
            log.warn("Submission rejected: duel {} is not ACTIVE (status={})", duelId, duel.getStatus());
            return;
        }

        boolean isPlayer = duel.getPlayer1().getId().equals(userId)
                || duel.getPlayer2().getId().equals(userId);
        if (!isPlayer) {
            log.warn("Submission rejected: user {} is not a participant of duel {}", userId, duelId);
            return;
        }

        log.info("Duel [{}]: submission from {}", duelId, username);

        // ─── EAGERLY EXTRACT lazy data while Hibernate session is still open ───
        Challenge challenge = duel.getChallenge();
        // Trigger initialization
        List<TestCase> testCases = challenge.getTestCases();
        int testCaseCount = testCases.size(); 
        DifficultyLevel difficulty = duel.getDifficulty();
        ProgrammingLanguage language = challenge.getLanguage();

        log.debug("Duel [{}]: executing {} test cases for {}", duelId, testCaseCount, username);

        CompletableFuture.supplyAsync(() -> {
            return codeExecutionService.execute(code, testCases, difficulty);
        }, executionPool).thenAccept(testResults -> {
            processSubmissionResults(duelId, userId, username, code, language, testResults);
        }).exceptionally(ex -> {
            log.error("Async code execution failed for duel {}: {}", duelId, ex.getMessage(), ex);
            return null;
        });
    }

    private void processSubmissionResults(UUID duelId, UUID userId, String username,
                                          String code, ProgrammingLanguage language,
                                          List<TestResultResponse> testResults) {
        transactionTemplate.executeWithoutResult(status -> {
            Duel duel = duelRepository.findById(duelId).orElse(null);
            if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
                log.info("Duel {} finished before submission could be processed", duelId);
                return;
            }

            Challenge challenge = duel.getChallenge();

            SubmissionStatus overallStatus = determineOverallStatus(testResults);
            int totalExecutionTimeMs = testResults.stream()
                    .mapToInt(TestResultResponse::getExecutionTimeMs)
                    .sum();

            User user = userRepository.getReferenceById(userId);
            Submission submission = Submission.builder()
                    .user(user)
                    .challenge(challenge)
                    .duel(duel)
                    .code(code)
                    .language(language)
                    .overallStatus(overallStatus)
                    .executionTimeMs(totalExecutionTimeMs)
                    .build();
            submission = submissionRepository.save(submission);

            int testsPassed = (int) testResults.stream()
                    .filter(tr -> tr.getStatus() == SubmissionStatus.ACCEPTED)
                    .count();
            int totalTests = testResults.size();

            DuelSubmissionResultMessage resultMsg = DuelSubmissionResultMessage.builder()
                    .submissionId(submission.getId().toString())
                    .overallStatus(overallStatus)
                    .executionTimeMs(totalExecutionTimeMs)
                    .testsPassedCount(testsPassed)
                    .totalTests(totalTests)
                    .testResults(testResults)
                    .build();
            messagingTemplate.convertAndSendToUser(username, "/queue/duel/result", resultMsg);

            int bestTestsPassed = calculateBestProgress(duelId, userId, testsPassed);

            DuelProgressMessage progressMsg = DuelProgressMessage.builder()
                    .duelId(duelId.toString())
                    .playerUsername(username)
                    .testsPassedCount(bestTestsPassed)
                    .totalTests(totalTests)
                    .lastActivity(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/duel/" + duelId + "/progress", progressMsg);

            if (overallStatus == SubmissionStatus.ACCEPTED) {
                synchronized (DuelService.this) {
                    Duel freshDuel = duelRepository.findById(duelId).orElse(null);
                    if (freshDuel != null && freshDuel.getStatus() == DuelStatus.ACTIVE) {
                        log.info("Duel [{}]: {} solved all tests! Declaring winner.", duelId, username);
                        finishDuelInternal(freshDuel, DuelFinishReason.SOLVED, userId);
                    }
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    // 3. DUEL FINISH (all paths converge here)
    // ──────────────────────────────────────────────

    @Transactional
    public void finishDuel(UUID duelId, DuelFinishReason reason, UUID winnerId) {
        Duel duel = duelRepository.findById(duelId).orElse(null);
        if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
            return;
        }
        finishDuelInternal(duel, reason, winnerId);
    }

    private void finishDuelInternal(Duel duel, DuelFinishReason reason, UUID winnerId) {
        UUID duelId = duel.getId();

        duel.setStatus(DuelStatus.FINISHED);
        duel.setFinishReason(reason);
        duel.setFinishedAt(LocalDateTime.now());

        User winner = null;
        if (winnerId != null) {
            winner = userRepository.findById(winnerId).orElse(null);
            duel.setWinner(winner);
        }
        duelRepository.save(duel);

        ScheduledFuture<?> timer = duelTimers.remove(duelId);
        if (timer != null) timer.cancel(false);

        activeDuelMap.remove(duel.getPlayer1().getId());
        activeDuelMap.remove(duel.getPlayer2().getId());

        int totalTests = duel.getChallenge().getTestCases().size();
        int p1Best = calculateBestProgress(duelId, duel.getPlayer1().getId(), 0);
        int p2Best = calculateBestProgress(duelId, duel.getPlayer2().getId(), 0);

        log.info("Duel [{}] FINISHED: reason={}, winner={}, p1={}/{}, p2={}/{}",
                duelId, reason,
                winner != null ? winner.getUsername() : "DRAW",
                p1Best, totalTests, p2Best, totalTests);

        DuelFinishedMessage finishedMsg = DuelFinishedMessage.builder()
                .duelId(duelId.toString())
                .winnerUsername(winner != null ? winner.getUsername() : null)
                .finishReason(reason)
                .player1TestsPassed(p1Best)
                .player2TestsPassed(p2Best)
                .player1Username(duel.getPlayer1().getUsername())
                .player2Username(duel.getPlayer2().getUsername())
                .build();
        messagingTemplate.convertAndSend("/topic/duel/" + duelId + "/finished", finishedMsg);
    }

    // ──────────────────────────────────────────────
    // 4. TIMEOUT HANDLER
    // ──────────────────────────────────────────────

    private void scheduleTimeout(UUID duelId) {
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            handleTimeout(duelId);
        }, DUEL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        duelTimers.put(duelId, timer);
    }

    private void handleTimeout(UUID duelId) {
        Duel duel = duelRepository.findById(duelId).orElse(null);
        if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
            return;
        }

        log.info("Duel [{}] timed out after {} minutes", duelId, DUEL_TIMEOUT_MINUTES);

        int p1Best = calculateBestProgress(duelId, duel.getPlayer1().getId(), 0);
        int p2Best = calculateBestProgress(duelId, duel.getPlayer2().getId(), 0);

        UUID winnerId = null;
        if (p1Best > p2Best) {
            winnerId = duel.getPlayer1().getId();
        } else if (p2Best > p1Best) {
            winnerId = duel.getPlayer2().getId();
        }

        finishDuel(duelId, DuelFinishReason.TIMEOUT, winnerId);
    }

    // ──────────────────────────────────────────────
    // 5. DISCONNECT / RECONNECT
    // ──────────────────────────────────────────────

    public void handlePlayerDisconnect(UUID userId) {
        UUID duelId = activeDuelMap.get(userId);
        if (duelId == null) return;

        Duel duel = duelRepository.findById(duelId).orElse(null);
        if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) return;

        UUID opponentId = duel.getPlayer1().getId().equals(userId)
                ? duel.getPlayer2().getId()
                : duel.getPlayer1().getId();

        log.info("Duel [{}]: player {} disconnected. Scheduling forfeit in {}s",
                duelId, userId, DISCONNECT_GRACE_SECONDS);

        ScheduledFuture<?> forfeit = scheduler.schedule(() -> {
            pendingForfeits.remove(userId);
            log.info("Duel [{}]: player {} forfeited (disconnect timeout)", duelId, userId);
            finishDuel(duelId, DuelFinishReason.FORFEIT, opponentId);
        }, DISCONNECT_GRACE_SECONDS, TimeUnit.SECONDS);

        pendingForfeits.put(userId, forfeit);
    }

    public void handlePlayerReconnect(UUID userId) {
        ScheduledFuture<?> pending = pendingForfeits.remove(userId);
        if (pending != null) {
            pending.cancel(false);
            log.info("Player {} reconnected. Cancelled pending forfeit.", userId);
        }
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    private int calculateBestProgress(UUID duelId, UUID userId, int currentSubmissionPassed) {
        List<Submission> submissions = submissionRepository
                .findByDuelIdAndUserIdOrderByCreatedAtDesc(duelId, userId);

        int best = currentSubmissionPassed;
        for (Submission s : submissions) {
            if (s.getOverallStatus() == SubmissionStatus.ACCEPTED) {
                return Integer.MAX_VALUE; 
            }
        }
        return best;
    }

    private SubmissionStatus determineOverallStatus(List<TestResultResponse> results) {
        boolean allAccepted = true;
        for (TestResultResponse result : results) {
            if (result.getStatus() == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
                return SubmissionStatus.TIME_LIMIT_EXCEEDED;
            }
            if (result.getStatus() == SubmissionStatus.RUNTIME_ERROR) {
                return SubmissionStatus.RUNTIME_ERROR;
            }
            if (result.getStatus() != SubmissionStatus.ACCEPTED) {
                allAccepted = false;
            }
        }
        return allAccepted ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER;
    }
}
