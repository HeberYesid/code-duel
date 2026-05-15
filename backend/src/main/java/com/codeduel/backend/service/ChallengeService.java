package com.codeduel.backend.service;

import com.codeduel.backend.dto.*;
import com.codeduel.backend.exception.BadRequestException;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.TestCase;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final TestCaseRepository testCaseRepository;

    /**
     * Get all challenges (summary view — no test case details).
     */
    public List<ChallengeResponse> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Get challenges filtered by difficulty.
     */
    public List<ChallengeResponse> getChallengesByDifficulty(DifficultyLevel difficulty) {
        return challengeRepository.findByDifficulty(difficulty).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Get a single challenge by ID with its test cases.
     */
    public ChallengeResponse getChallengeById(UUID id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + id));
        return toDetailResponse(challenge);
    }

    /**
     * Get a random challenge by difficulty (used by matchmaking).
     */
    public ChallengeResponse getRandomChallenge(DifficultyLevel difficulty) {
        Challenge challenge = challengeRepository.findRandomByDifficulty(difficulty.name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No challenges found for difficulty: " + difficulty));
        return toDetailResponse(challenge);
    }

    /**
     * Create a new challenge with test cases.
     */
    @Transactional
    public ChallengeResponse createChallenge(ChallengeRequest request) {
        if (challengeRepository.existsByTitle(request.getTitle())) {
            throw new BadRequestException("A challenge with this title already exists");
        }

        Challenge challenge = Challenge.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .difficulty(request.getDifficulty())
                .language(request.getLanguage())
                .build();

        for (TestCaseRequest tcReq : request.getTestCases()) {
            TestCase testCase = TestCase.builder()
                    .input(tcReq.getInput())
                    .expectedOutput(tcReq.getExpectedOutput())
                    .testOrder(tcReq.getTestOrder())
                    .build();
            challenge.addTestCase(testCase);
        }

        challenge = challengeRepository.save(challenge);
        return toDetailResponse(challenge);
    }

    /**
     * Delete a challenge by ID.
     */
    @Transactional
    public void deleteChallenge(UUID id) {
        if (!challengeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Challenge not found with id: " + id);
        }
        challengeRepository.deleteById(id);
    }

    // ==================== Mappers ====================

    private ChallengeResponse toSummaryResponse(Challenge challenge) {
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .difficulty(challenge.getDifficulty())
                .language(challenge.getLanguage())
                .testCaseCount(testCaseRepository.countByChallengeId(challenge.getId()))
                .build();
    }

    private ChallengeResponse toDetailResponse(Challenge challenge) {
        List<TestCaseResponse> testCases = challenge.getTestCases().stream()
                .map(tc -> TestCaseResponse.builder()
                        .id(tc.getId())
                        .input(tc.getInput())
                        .expectedOutput(tc.getExpectedOutput())
                        .testOrder(tc.getTestOrder())
                        .build())
                .toList();

        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .difficulty(challenge.getDifficulty())
                .language(challenge.getLanguage())
                .testCaseCount(testCases.size())
                .testCases(testCases)
                .build();
    }
}
