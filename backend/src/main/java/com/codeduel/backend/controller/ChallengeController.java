package com.codeduel.backend.controller;

import com.codeduel.backend.dto.ChallengeRequest;
import com.codeduel.backend.dto.ChallengeResponse;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getAllChallenges(
            @RequestParam(required = false) DifficultyLevel difficulty) {
        List<ChallengeResponse> challenges;
        if (difficulty != null) {
            challenges = challengeService.getChallengesByDifficulty(difficulty);
        } else {
            challenges = challengeService.getAllChallenges();
        }
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeResponse> getChallengeById(@PathVariable UUID id) {
        return ResponseEntity.ok(challengeService.getChallengeById(id));
    }

    @GetMapping("/random")
    public ResponseEntity<ChallengeResponse> getRandomChallenge(
            @RequestParam DifficultyLevel difficulty) {
        return ResponseEntity.ok(challengeService.getRandomChallenge(difficulty));
    }

    @PostMapping
    public ResponseEntity<ChallengeResponse> createChallenge(
            @Valid @RequestBody ChallengeRequest request) {
        ChallengeResponse response = challengeService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable UUID id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }
}
