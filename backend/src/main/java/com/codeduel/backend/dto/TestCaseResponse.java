package com.codeduel.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResponse {
    private UUID id;
    private String input;
    private String expectedOutput;
    private Integer testOrder;
}
