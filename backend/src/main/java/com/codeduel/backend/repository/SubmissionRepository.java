package com.codeduel.backend.repository;

import com.codeduel.backend.model.Submission;
import com.codeduel.backend.model.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Submission> findByUserIdAndChallengeIdOrderByCreatedAtDesc(UUID userId, UUID challengeId);

    /** Find all submissions for a player within a specific duel */
    List<Submission> findByDuelIdAndUserIdOrderByCreatedAtDesc(UUID duelId, UUID userId);

    /** Count submissions with a specific status for a player in a duel (e.g. ACCEPTED) */
    long countByDuelIdAndUserIdAndOverallStatus(UUID duelId, UUID userId, SubmissionStatus status);
}
