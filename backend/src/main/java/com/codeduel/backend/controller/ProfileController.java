package com.codeduel.backend.controller;

import com.codeduel.backend.dto.PlayerStatsResponse;
import com.codeduel.backend.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final RankingService rankingService;

    @GetMapping("/me/stats")
    public ResponseEntity<PlayerStatsResponse> getMyStats(Authentication authentication) {
        return ResponseEntity.ok(rankingService.getPlayerStats(authentication.getName()));
    }
}
