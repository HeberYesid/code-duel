package com.codeduel.backend.controller;

import com.codeduel.backend.dto.AuthResponse;
import com.codeduel.backend.dto.RegisterRequest;
import com.codeduel.backend.model.Notification;
import com.codeduel.backend.model.ScoreEntry;
import com.codeduel.backend.model.User;
import com.codeduel.backend.model.enums.NotificationType;
import com.codeduel.backend.repository.NotificationRepository;
import com.codeduel.backend.repository.ProfileRepository;
import com.codeduel.backend.repository.ScoreEntryRepository;
import com.codeduel.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Competitive Controllers Integration Tests")
class CompetitiveControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScoreEntryRepository scoreEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void cleanDb() {
        notificationRepository.deleteAll();
        scoreEntryRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/profile/me/stats returns the authenticated user competitive stats")
    void getMyStats_ShouldReturnAuthenticatedUserStats() throws Exception {
        AuthResponse auth = register("player1", "player1@test.com");
        ScoreEntry scoreEntry = scoreEntryRepository.findByUserId(auth.getUserId()).orElseThrow();
        scoreEntry.setElo(1188);
        scoreEntry.setWins(7);
        scoreEntry.setLosses(3);
        scoreEntry.setDraws(2);
        scoreEntry.setDuelsPlayed(12);
        scoreEntryRepository.save(scoreEntry);

        mockMvc.perform(get("/api/profile/me/stats")
                        .header("Authorization", "Bearer " + auth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("player1")))
                .andExpect(jsonPath("$.elo", is(1188)))
                .andExpect(jsonPath("$.wins", is(7)))
                .andExpect(jsonPath("$.losses", is(3)))
                .andExpect(jsonPath("$.draws", is(2)))
                .andExpect(jsonPath("$.duelsPlayed", is(12)));
    }

    @Test
    @DisplayName("GET /api/profile/me/stats creates a default score entry for legacy users missing ranking data")
    void getMyStats_WhenLegacyUserHasNoScoreEntry_ShouldReturnDefaultStats() throws Exception {
        AuthResponse auth = register("legacy", "legacy@test.com");
        ScoreEntry existingScore = scoreEntryRepository.findByUserId(auth.getUserId()).orElseThrow();
        scoreEntryRepository.delete(existingScore);

        mockMvc.perform(get("/api/profile/me/stats")
                        .header("Authorization", "Bearer " + auth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("legacy")))
                .andExpect(jsonPath("$.elo", is(1000)))
                .andExpect(jsonPath("$.wins", is(0)))
                .andExpect(jsonPath("$.losses", is(0)))
                .andExpect(jsonPath("$.draws", is(0)))
                .andExpect(jsonPath("$.duelsPlayed", is(0)));
    }

    @Test
    @DisplayName("GET /api/leaderboard returns top players and highlights current user outside top limit")
    void getLeaderboard_ShouldReturnTopEntriesAndCurrentUserContext() throws Exception {
        AuthResponse currentUser = register("current", "current@test.com");
        ScoreEntry currentScore = scoreEntryRepository.findByUserId(currentUser.getUserId()).orElseThrow();
        currentScore.setElo(900);
        scoreEntryRepository.save(currentScore);

        for (int i = 0; i < 11; i++) {
            User user = userRepository.save(User.builder()
                    .username("top" + i)
                    .email("top" + i + "@test.com")
                    .passwordHash("hash")
                    .build());
            scoreEntryRepository.save(ScoreEntry.builder()
                    .user(user)
                    .elo(1200 - (i * 10))
                    .wins(10 - i)
                    .losses(i)
                    .draws(0)
                    .duelsPlayed(10)
                    .build());
        }

        mockMvc.perform(get("/api/leaderboard?limit=10")
                        .header("Authorization", "Bearer " + currentUser.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(10)))
                .andExpect(jsonPath("$.entries[0].username", is("top0")))
                .andExpect(jsonPath("$.entries[0].rank", is(1)))
                .andExpect(jsonPath("$.currentUser.username", is("current")))
                .andExpect(jsonPath("$.currentUser.rank", is(12)));
    }

    @Test
    @DisplayName("GET /api/notifications returns unread count and notifications ordered newest first")
    void getNotifications_ShouldReturnUnreadCountAndOrderedNotifications() throws Exception {
        AuthResponse auth = register("notifyme", "notify@test.com");
        User user = userRepository.findById(auth.getUserId()).orElseThrow();

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.RANK_CHANGE)
                .title("Older")
                .message("Old message")
                .oldElo(1000)
                .newElo(1016)
                .read(true)
                .build());
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.RANK_CHANGE)
                .title("Newest")
                .message("New message")
                .oldElo(1016)
                .newElo(1032)
                .read(false)
                .build());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + auth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)))
                .andExpect(jsonPath("$.notifications", hasSize(2)))
                .andExpect(jsonPath("$.notifications[0].title", is("Newest")))
                .andExpect(jsonPath("$.notifications[0].read", is(false)));
    }

    @Test
    @DisplayName("POST /api/notifications/mark-all-read marks every unread notification for the current user")
    void markAllNotificationsRead_ShouldMarkUnreadNotifications() throws Exception {
        AuthResponse auth = register("reader", "reader@test.com");
        User user = userRepository.findById(auth.getUserId()).orElseThrow();

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.RANK_CHANGE)
                .title("First")
                .message("First message")
                .oldElo(1000)
                .newElo(1016)
                .read(false)
                .build());
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.RANK_CHANGE)
                .title("Second")
                .message("Second message")
                .oldElo(1016)
                .newElo(1032)
                .read(false)
                .build());

        mockMvc.perform(post("/api/notifications/mark-all-read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount", is(2)))
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }

    private AuthResponse register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(username, email, "Pass123");
        String content = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(content, AuthResponse.class);
    }
}
