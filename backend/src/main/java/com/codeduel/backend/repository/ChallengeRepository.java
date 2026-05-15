package com.codeduel.backend.repository;

import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.enums.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    List<Challenge> findByDifficulty(DifficultyLevel difficulty);

    @Query(value = "SELECT * FROM challenges WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT 1",
            nativeQuery = true)
    Optional<Challenge> findRandomByDifficulty(@Param("difficulty") String difficulty);

    boolean existsByTitle(String title);
}
