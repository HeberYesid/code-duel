package com.codeduel.backend.controller;

import com.codeduel.backend.dto.ChallengeRequest;
import com.codeduel.backend.dto.TestCaseRequest;
import com.codeduel.backend.model.Challenge;
import com.codeduel.backend.model.TestCase;
import com.codeduel.backend.model.enums.DifficultyLevel;
import com.codeduel.backend.model.enums.ProgrammingLanguage;
import com.codeduel.backend.repository.ChallengeRepository;
import com.codeduel.backend.repository.UserRepository;
import com.codeduel.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ChallengeController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChallengeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChallengeRepository challengeRepository;
    @Autowired
    private JwtService jwtService;

    private String authToken;

    @BeforeEach
    void setUp() {
        challengeRepository.deleteAll();
        // Generate a valid JWT for authenticated requests
        authToken = "Bearer " + jwtService.generateToken(UUID.randomUUID(), "testadmin");
    }

    private Challenge seedChallenge(String title, DifficultyLevel difficulty) {
        Challenge challenge = Challenge.builder()
                .title(title)
                .description("Description for " + title)
                .difficulty(difficulty)
                .language(ProgrammingLanguage.PYTHON)
                .build();
        TestCase tc = TestCase.builder()
                .input("1")
                .expectedOutput("1")
                .testOrder(1)
                .build();
        challenge.addTestCase(tc);
        return challengeRepository.save(challenge);
    }

    // ==================== GET ALL ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/challenges — 200 returns all challenges")
    void getAll_ShouldReturnList() throws Exception {
        seedChallenge("Challenge A", DifficultyLevel.EASY);
        seedChallenge("Challenge B", DifficultyLevel.HARD);

        mockMvc.perform(get("/api/challenges")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].testCases").doesNotExist());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/challenges?difficulty=EASY — 200 filters by difficulty")
    void getAll_WithDifficultyFilter_ShouldFilterCorrectly() throws Exception {
        seedChallenge("Easy One", DifficultyLevel.EASY);
        seedChallenge("Hard One", DifficultyLevel.HARD);

        mockMvc.perform(get("/api/challenges")
                        .param("difficulty", "EASY")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/challenges — 200 returns empty list when none exist")
    void getAll_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/challenges")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== GET BY ID ====================

    @Test
    @Order(4)
    @DisplayName("GET /api/challenges/{id} — 200 returns challenge with test cases")
    void getById_WhenExists_ShouldReturnDetail() throws Exception {
        Challenge saved = seedChallenge("Detail Test", DifficultyLevel.MEDIUM);

        mockMvc.perform(get("/api/challenges/" + saved.getId())
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Detail Test"))
                .andExpect(jsonPath("$.testCases", hasSize(1)))
                .andExpect(jsonPath("$.testCases[0].input").value("1"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/challenges/{id} — 404 when not found")
    void getById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/challenges/" + UUID.randomUUID())
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    // ==================== GET RANDOM ====================

    @Test
    @Order(6)
    @DisplayName("GET /api/challenges/random?difficulty=EASY — 200 returns a challenge")
    void getRandom_WhenExists_ShouldReturnChallenge() throws Exception {
        seedChallenge("Random Candidate", DifficultyLevel.EASY);

        mockMvc.perform(get("/api/challenges/random")
                        .param("difficulty", "EASY")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.testCases").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/challenges/random?difficulty=HARD — 404 when none exist")
    void getRandom_WhenNoneExist_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/challenges/random")
                        .param("difficulty", "HARD")
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE ====================

    @Test
    @Order(8)
    @DisplayName("POST /api/challenges — 201 creates challenge with test cases")
    void create_WithValidData_ShouldReturn201() throws Exception {
        ChallengeRequest request = ChallengeRequest.builder()
                .title("New Problem")
                .description("Solve this problem")
                .difficulty(DifficultyLevel.MEDIUM)
                .language(ProgrammingLanguage.PYTHON)
                .testCases(List.of(
                        TestCaseRequest.builder().input("hello").expectedOutput("olleh").testOrder(1).build(),
                        TestCaseRequest.builder().input("world").expectedOutput("dlrow").testOrder(2).build()
                ))
                .build();

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("New Problem"))
                .andExpect(jsonPath("$.testCases", hasSize(2)));
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/challenges — 400 with missing title")
    void create_WithMissingTitle_ShouldReturn400() throws Exception {
        ChallengeRequest request = ChallengeRequest.builder()
                .description("No title")
                .difficulty(DifficultyLevel.EASY)
                .language(ProgrammingLanguage.PYTHON)
                .testCases(List.of(
                        TestCaseRequest.builder().input("1").expectedOutput("1").testOrder(1).build()
                ))
                .build();

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/challenges — 400 with no test cases")
    void create_WithNoTestCases_ShouldReturn400() throws Exception {
        ChallengeRequest request = ChallengeRequest.builder()
                .title("No Tests")
                .description("Missing test cases")
                .difficulty(DifficultyLevel.EASY)
                .language(ProgrammingLanguage.PYTHON)
                .testCases(List.of())
                .build();

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/challenges — 400 with duplicate title")
    void create_WithDuplicateTitle_ShouldReturn400() throws Exception {
        seedChallenge("Duplicate Title", DifficultyLevel.EASY);

        ChallengeRequest request = ChallengeRequest.builder()
                .title("Duplicate Title")
                .description("Same title")
                .difficulty(DifficultyLevel.EASY)
                .language(ProgrammingLanguage.PYTHON)
                .testCases(List.of(
                        TestCaseRequest.builder().input("1").expectedOutput("1").testOrder(1).build()
                ))
                .build();

        mockMvc.perform(post("/api/challenges")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // ==================== DELETE ====================

    @Test
    @Order(12)
    @DisplayName("DELETE /api/challenges/{id} — 204 when exists")
    void delete_WhenExists_ShouldReturn204() throws Exception {
        Challenge saved = seedChallenge("To Delete", DifficultyLevel.EASY);

        mockMvc.perform(delete("/api/challenges/" + saved.getId())
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /api/challenges/{id} — 404 when not found")
    void delete_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/challenges/" + UUID.randomUUID())
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    // ==================== SECURITY ====================

    @Test
    @Order(14)
    @DisplayName("GET /api/challenges — 403 without auth token")
    void getAll_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/challenges"))
                .andExpect(status().isForbidden());
    }
}
