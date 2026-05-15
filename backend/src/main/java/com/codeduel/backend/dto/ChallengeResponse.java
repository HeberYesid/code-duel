package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {
    private UUID id;
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    private ProgrammingLanguage language;
    private int testCaseCount;
    private List<TestCaseResponse> testCases;
}
