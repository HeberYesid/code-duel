package com.codeduel.backend.service;

import com.codeduel.backend.dto.SubmissionRequest;
import com.codeduel.backend.dto.SubmissionResponse;
import com.codeduel.backend.dto.TestResultResponse;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.Submission;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.SubmissionStatus;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.SubmissionRepository;
import com.codeduel.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final CodeExecutionService codeExecutionService;

    /**
     * Submits code for a challenge in practice mode.
     * Executes code, persists the submission, and returns results with full details.
     */
    @Transactional
    public SubmissionResponse submitPractice(SubmissionRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Challenge not found with id: " + request.getChallengeId()));

        log.info("Practice submission by user={} for challenge={}", user.getUsername(), challenge.getTitle());

        // Execute code against all test cases
        List<TestResultResponse> testResults = codeExecutionService.execute(
                request.getCode(),
                challenge.getTestCases(),
                challenge.getDifficulty()
        );

        // Determine overall status
        SubmissionStatus overallStatus = determineOverallStatus(testResults);

        // Calculate total execution time
        int totalExecutionTimeMs = testResults.stream()
                .mapToInt(TestResultResponse::getExecutionTimeMs)
                .sum();

        // Persist submission
        Submission submission = Submission.builder()
                .user(user)
                .challenge(challenge)
                .code(request.getCode())
                .language(challenge.getLanguage())
                .overallStatus(overallStatus)
                .executionTimeMs(totalExecutionTimeMs)
                .build();
        submission = submissionRepository.save(submission);

        return toResponse(submission, challenge.getTitle(), testResults);
    }

    /**
     * Get all submissions for the authenticated user.
     */
    public List<SubmissionResponse> getUserSubmissions(UUID userId) {
        return submissionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> toResponse(s, s.getChallenge().getTitle(), null))
                .toList();
    }

    /**
     * Get submissions for a specific challenge by the authenticated user.
     */
    public List<SubmissionResponse> getUserSubmissionsByChallenge(UUID userId, UUID challengeId) {
        if (!challengeRepository.existsById(challengeId)) {
            throw new ResourceNotFoundException("Challenge not found with id: " + challengeId);
        }
        return submissionRepository.findByUserIdAndChallengeIdOrderByCreatedAtDesc(userId, challengeId).stream()
                .map(s -> toResponse(s, s.getChallenge().getTitle(), null))
                .toList();
    }

    /**
     * Determines overall status based on individual test results.
     * Priority: TIME_LIMIT > RUNTIME_ERROR > WRONG_ANSWER > ACCEPTED
     */
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

    private SubmissionResponse toResponse(Submission submission, String challengeTitle,
                                          List<TestResultResponse> testResults) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .challengeId(submission.getChallenge().getId())
                .challengeTitle(challengeTitle)
                .language(submission.getLanguage())
                .overallStatus(submission.getOverallStatus())
                .executionTimeMs(submission.getExecutionTimeMs())
                .createdAt(submission.getCreatedAt())
                .testResults(testResults)
                .build();
    }
}
