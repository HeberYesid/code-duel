package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.ProgrammingLanguage;
import com.codeduel.backend.model.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private UUID id;
    private UUID challengeId;
    private String challengeTitle;
    private ProgrammingLanguage language;
    private SubmissionStatus overallStatus;
    private Integer executionTimeMs;
    private LocalDateTime createdAt;
    private List<TestResultResponse> testResults;
}
