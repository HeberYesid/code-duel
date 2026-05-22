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
public class PlayerStatsResponse {
    private UUID userId;
    private String username;
    private Integer elo;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private Integer duelsPlayed;
}
