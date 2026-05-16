package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.SubmissionStatus;
import lombok.*;

import java.util.List;

/**
 * STOMP message sent privately to a player with their submission result.
 * Destination: /user/queue/duel/result
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelSubmissionResultMessage {
    private String submissionId;
    private SubmissionStatus overallStatus;
    private int executionTimeMs;
    private int testsPassedCount;
    private int totalTests;
    private List<TestResultResponse> testResults;
}
