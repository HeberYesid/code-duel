package com.codeduel.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRequest {

    @NotBlank(message = "Test case input is required")
    private String input;

    @NotBlank(message = "Expected output is required")
    private String expectedOutput;

    @NotNull(message = "Test order is required")
    @Positive(message = "Test order must be a positive number")
    private Integer testOrder;
}
