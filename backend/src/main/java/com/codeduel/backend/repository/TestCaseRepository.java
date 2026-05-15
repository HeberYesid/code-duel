package com.codeduel.backend.repository;

import com.codeduel.backend.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByChallengeIdOrderByTestOrderAsc(UUID challengeId);

    int countByChallengeId(UUID challengeId);
}
