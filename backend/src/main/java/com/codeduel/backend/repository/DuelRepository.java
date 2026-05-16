package com.codeduel.backend.repository;

import com.codeduel.backend.model.Duel;
import com.codeduel.backend.model.enums.DuelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DuelRepository extends JpaRepository<Duel, UUID> {

    /**
     * Find an active duel where the given user is either player1 or player2.
     */
    @Query("SELECT d FROM Duel d WHERE d.status = :status AND (d.player1.id = :userId OR d.player2.id = :userId)")
    Optional<Duel> findByStatusAndPlayer(
            @Param("status") DuelStatus status,
            @Param("userId") UUID userId
    );

    /**
     * Convenience: find active duel for a user.
     */
    default Optional<Duel> findActiveByUserId(UUID userId) {
        return findByStatusAndPlayer(DuelStatus.ACTIVE, userId);
    }
}
