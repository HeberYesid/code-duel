package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {
    private int testOrder;
    private SubmissionStatus status;
    private int executionTimeMs;
    private String input;
    private String expected;
    private String actual;
    private String stderr;
}
