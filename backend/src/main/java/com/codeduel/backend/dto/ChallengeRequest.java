package com.codeduel.backend.dto;

import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.ProgrammingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Difficulty is required")
    private DifficultyLevel difficulty;

    @NotNull(message = "Language is required")
    private ProgrammingLanguage language;

    @NotEmpty(message = "At least one test case is required")
    @Valid
    private List<TestCaseRequest> testCases;
}
