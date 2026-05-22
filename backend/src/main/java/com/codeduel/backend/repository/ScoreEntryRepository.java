package com.codeduel.backend.repository;

import com.codeduel.backend.model.ScoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoreEntryRepository extends JpaRepository<ScoreEntry, UUID> {
    Optional<ScoreEntry> findByUserId(UUID userId);

    @Query("select s from ScoreEntry s join fetch s.user order by s.elo desc, s.wins desc, s.user.username asc")
    List<ScoreEntry> findLeaderboardOrdered();
}
