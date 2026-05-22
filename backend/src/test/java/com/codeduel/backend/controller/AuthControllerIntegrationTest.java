package com.codeduel.backend.controller;

import com.codeduel.backend.dto.LoginRequest;
import com.codeduel.backend.dto.RegisterRequest;
import com.codeduel.backend.repository.NotificationRepository;
import com.codeduel.backend.repository.ProfileRepository;
import com.codeduel.backend.repository.ScoreEntryRepository;
import com.codeduel.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private ScoreEntryRepository scoreEntryRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanDb() {
        notificationRepository.deleteAll();
        scoreEntryRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/register — 201 with valid data")
    void register_WithValidData_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("player1", "player1@test.com", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("player1"))
                .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/register — 400 with blank username")
    void register_WithBlankUsername_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "test@test.com", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/register — 400 with invalid email format")
    void register_WithInvalidEmail_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("player1", "not-an-email", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/register — 400 with weak password (no uppercase)")
    void register_WithWeakPassword_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("player1", "p@test.com", "password1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/register — 400 with short password")
    void register_WithShortPassword_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("player1", "p@test.com", "Ab1");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/auth/register — 400 with username containing special chars")
    void register_WithInvalidUsernameChars_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("user@name!", "p@test.com", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/auth/register — 400 with duplicate username")
    void register_WithDuplicateUsername_ShouldReturn400() throws Exception {
        RegisterRequest first = new RegisterRequest("samename", "first@test.com", "Pass123");
        RegisterRequest second = new RegisterRequest("samename", "second@test.com", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/auth/register — 400 with duplicate email")
    void register_WithDuplicateEmail_ShouldReturn400() throws Exception {
        RegisterRequest first = new RegisterRequest("user1", "same@test.com", "Pass123");
        RegisterRequest second = new RegisterRequest("user2", "same@test.com", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @Order(9)
    @DisplayName("POST /api/auth/login — 200 with valid credentials")
    void login_WithValidCredentials_ShouldReturn200() throws Exception {
        // First register
        RegisterRequest reg = new RegisterRequest("loginuser", "login@test.com", "Pass123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest login = new LoginRequest("loginuser", "Pass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/auth/login — 401 with wrong password")
    void login_WithWrongPassword_ShouldReturn401() throws Exception {
        RegisterRequest reg = new RegisterRequest("wrongpwd", "wrong@test.com", "Pass123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("wrongpwd", "WrongPassword1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/auth/login — 401 with non-existent user")
    void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        LoginRequest login = new LoginRequest("ghostuser", "Pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("POST /api/auth/login — 400 with blank fields")
    void login_WithBlankFields_ShouldReturn400() throws Exception {
        LoginRequest login = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @Order(13)
    @DisplayName("GET /api/protected — 403 without token")
    void protectedEndpoint_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/some-protected-resource")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    @DisplayName("POST /api/auth/register — normalizes uppercase username/email")
    void register_WithUppercaseUsernameAndEmail_ShouldNormalize() throws Exception {
        RegisterRequest request = new RegisterRequest("NewUser", "NewUser@Example.COM", "Pass123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @Order(15)
    @DisplayName("POST /api/auth/login — accepts uppercase username and normalizes")
    void login_WithUppercaseUsername_ShouldReturn200() throws Exception {
        RegisterRequest register = new RegisterRequest("caseuser", "case@test.com", "Pass123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("CASEUSER", "Pass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("caseuser"));
    }

    @Test
    @Order(16)
    @DisplayName("POST /api/auth/login — 400 with invalid username characters")
    void login_WithInvalidUsernameChars_ShouldReturn400() throws Exception {
        LoginRequest login = new LoginRequest("invalid@name", "Pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem(containsString("Username can only contain letters, numbers and underscores"))));
    }

    @Test
    @Order(17)
    @DisplayName("POST /api/auth/login — 400 with short password")
    void login_WithShortPassword_ShouldReturn400() throws Exception {
        LoginRequest login = new LoginRequest("validuser", "Ab1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasItem(containsString("Password must be between 6 and 100 characters"))));
    }
}
