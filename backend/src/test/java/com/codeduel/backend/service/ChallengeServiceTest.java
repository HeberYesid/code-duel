package com.codeduel.backend.service;

import com.codeduel.backend.dto.*;
import com.codeduel.backend.exception.BadRequestException;
import com.codeduel.backend.exception.ResourceNotFoundException;
import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.TestCase;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.ProgrammingLanguage;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService Unit Tests")
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private ChallengeService challengeService;

    private Challenge sampleChallenge;
    private UUID challengeId;

    @BeforeEach
    void setUp() {
        challengeId = UUID.randomUUID();
        sampleChallenge = Challenge.builder()
                .id(challengeId)
                .title("Two Sum")
                .description("Find two numbers that add up to target")
                .difficulty(DifficultyLevel.EASY)
                .language(ProgrammingLanguage.PYTHON)
                .build();

        TestCase tc1 = TestCase.builder()
                .id(UUID.randomUUID())
                .input("2 7 11 15\n9")
                .expectedOutput("0 1")
                .testOrder(1)
                .challenge(sampleChallenge)
                .build();
        TestCase tc2 = TestCase.builder()
                .id(UUID.randomUUID())
                .input("3 2 4\n6")
                .expectedOutput("1 2")
                .testOrder(2)
                .challenge(sampleChallenge)
                .build();
        sampleChallenge.setTestCases(List.of(tc1, tc2));
    }

    @Nested
    @DisplayName("getAllChallenges")
    class GetAllTests {

        @Test
        @DisplayName("Should return all challenges as summary responses")
        void getAllChallenges_ShouldReturnList() {
            when(challengeRepository.findAll()).thenReturn(List.of(sampleChallenge));
            when(testCaseRepository.countByChallengeId(challengeId)).thenReturn(2);

            List<ChallengeResponse> result = challengeService.getAllChallenges();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Two Sum");
            assertThat(result.get(0).getTestCaseCount()).isEqualTo(2);
            assertThat(result.get(0).getTestCases()).isNull(); // summary has no test case details
        }

        @Test
        @DisplayName("Should return empty list when no challenges exist")
        void getAllChallenges_WhenEmpty_ShouldReturnEmptyList() {
            when(challengeRepository.findAll()).thenReturn(List.of());

            List<ChallengeResponse> result = challengeService.getAllChallenges();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChallengesByDifficulty")
    class GetByDifficultyTests {

        @Test
        @DisplayName("Should return only challenges matching the difficulty")
        void getChallengesByDifficulty_ShouldFilterCorrectly() {
            when(challengeRepository.findByDifficulty(DifficultyLevel.EASY))
                    .thenReturn(List.of(sampleChallenge));
            when(testCaseRepository.countByChallengeId(challengeId)).thenReturn(2);

            List<ChallengeResponse> result = challengeService.getChallengesByDifficulty(DifficultyLevel.EASY);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDifficulty()).isEqualTo(DifficultyLevel.EASY);
        }
    }

    @Nested
    @DisplayName("getChallengeById")
    class GetByIdTests {

        @Test
        @DisplayName("Should return challenge with test cases when found")
        void getChallengeById_WhenExists_ShouldReturnDetailResponse() {
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(sampleChallenge));

            ChallengeResponse result = challengeService.getChallengeById(challengeId);

            assertThat(result.getId()).isEqualTo(challengeId);
            assertThat(result.getTestCases()).hasSize(2);
            assertThat(result.getTestCases().get(0).getInput()).isEqualTo("2 7 11 15\n9");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void getChallengeById_WhenNotExists_ShouldThrow() {
            UUID randomId = UUID.randomUUID();
            when(challengeRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getChallengeById(randomId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Challenge not found");
        }
    }

    @Nested
    @DisplayName("getRandomChallenge")
    class GetRandomTests {

        @Test
        @DisplayName("Should return a random challenge for given difficulty")
        void getRandomChallenge_ShouldReturnChallenge() {
            when(challengeRepository.findRandomByDifficulty("EASY"))
                    .thenReturn(Optional.of(sampleChallenge));

            ChallengeResponse result = challengeService.getRandomChallenge(DifficultyLevel.EASY);

            assertThat(result).isNotNull();
            assertThat(result.getDifficulty()).isEqualTo(DifficultyLevel.EASY);
        }

        @Test
        @DisplayName("Should throw when no challenges exist for difficulty")
        void getRandomChallenge_WhenNoneExist_ShouldThrow() {
            when(challengeRepository.findRandomByDifficulty("HARD"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> challengeService.getRandomChallenge(DifficultyLevel.HARD))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No challenges found");
        }
    }

    @Nested
    @DisplayName("createChallenge")
    class CreateTests {

        @Test
        @DisplayName("Should create challenge with test cases successfully")
        void createChallenge_WithValidData_ShouldReturnResponse() {
            ChallengeRequest request = ChallengeRequest.builder()
                    .title("New Challenge")
                    .description("Description here")
                    .difficulty(DifficultyLevel.MEDIUM)
                    .language(ProgrammingLanguage.PYTHON)
                    .testCases(List.of(
                            TestCaseRequest.builder().input("1").expectedOutput("2").testOrder(1).build()
                    ))
                    .build();

            when(challengeRepository.existsByTitle(anyString())).thenReturn(false);
            when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> {
                Challenge c = invocation.getArgument(0);
                c.setId(UUID.randomUUID());
                c.getTestCases().forEach(tc -> tc.setId(UUID.randomUUID()));
                return c;
            });

            ChallengeResponse result = challengeService.createChallenge(request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Challenge");
            assertThat(result.getTestCases()).hasSize(1);
            verify(challengeRepository).save(any(Challenge.class));
        }

        @Test
        @DisplayName("Should throw when title already exists")
        void createChallenge_WithDuplicateTitle_ShouldThrow() {
            ChallengeRequest request = ChallengeRequest.builder()
                    .title("Existing Title")
                    .description("Desc")
                    .difficulty(DifficultyLevel.EASY)
                    .language(ProgrammingLanguage.PYTHON)
                    .testCases(List.of(
                            TestCaseRequest.builder().input("1").expectedOutput("1").testOrder(1).build()
                    ))
                    .build();

            when(challengeRepository.existsByTitle("Existing Title")).thenReturn(true);

            assertThatThrownBy(() -> challengeService.createChallenge(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already exists");

            verify(challengeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteChallenge")
    class DeleteTests {

        @Test
        @DisplayName("Should delete challenge when it exists")
        void deleteChallenge_WhenExists_ShouldDelete() {
            when(challengeRepository.existsById(challengeId)).thenReturn(true);

            challengeService.deleteChallenge(challengeId);

            verify(challengeRepository).deleteById(challengeId);
        }

        @Test
        @DisplayName("Should throw when challenge does not exist")
        void deleteChallenge_WhenNotExists_ShouldThrow() {
            UUID randomId = UUID.randomUUID();
            when(challengeRepository.existsById(randomId)).thenReturn(false);

            assertThatThrownBy(() -> challengeService.deleteChallenge(randomId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(challengeRepository, never()).deleteById(any());
        }
    }
}
