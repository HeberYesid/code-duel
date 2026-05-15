package com.codeduel.backend.model;

import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgrammingLanguage language;

    @Column(name = "room_code", length = 50)
    private String roomCode;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("testOrder ASC")
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    /**
     * Helper method to add a test case maintaining the bidirectional relationship.
     */
    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
        testCase.setChallenge(this);
    }
}
