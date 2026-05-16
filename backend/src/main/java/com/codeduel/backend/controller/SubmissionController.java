package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SubmissionRequest;
import com.codeduel.backend.dto.SubmissionResponse;
import com.codeduel.backend.security.JwtService;
import com.codeduel.backend.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final JwtService jwtService;

    /**
     * Submit code for practice mode. Executes code and returns detailed results.
     */
    @PostMapping("/practice")
    public ResponseEntity<SubmissionResponse> submitPractice(
            @Valid @RequestBody SubmissionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        SubmissionResponse response = submissionService.submitPractice(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all submissions for the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(submissionService.getUserSubmissions(userId));
    }

    /**
     * Get submissions for a specific challenge by the authenticated user.
     */
    @GetMapping("/me/{challengeId}")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissionsByChallenge(
            @PathVariable UUID challengeId,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(submissionService.getUserSubmissionsByChallenge(userId, challengeId));
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        return jwtService.extractUserId(token);
    }
}
