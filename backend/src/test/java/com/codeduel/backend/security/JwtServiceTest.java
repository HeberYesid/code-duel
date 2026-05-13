package com.codeduel.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);
    }

    @Nested
    @DisplayName("Token Generation")
    class GenerationTests {

        @Test
        @DisplayName("Should generate a non-null token")
        void generateToken_ShouldReturnNonNullToken() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Generated token should contain correct username")
        void generateToken_ShouldContainCorrectUsername() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            String extractedUsername = jwtService.extractUsername(token);
            assertThat(extractedUsername).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Generated token should contain correct userId")
        void generateToken_ShouldContainCorrectUserId() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            UUID extractedId = jwtService.extractUserId(token);
            assertThat(extractedId).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should validate a valid token")
        void isTokenValid_WithValidToken_ShouldReturnTrue() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("Should reject token with wrong username")
        void isTokenValid_WithWrongUsername_ShouldReturnFalse() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "testuser");

            UserDetails userDetails = new User("otheruser", "password", Collections.emptyList());
            assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
            // Create a JwtService with 0ms expiration
            JwtService shortLivedService = new JwtService();
            ReflectionTestUtils.setField(shortLivedService, "secretKey", TEST_SECRET);
            ReflectionTestUtils.setField(shortLivedService, "jwtExpiration", 0L);

            UUID userId = UUID.randomUUID();
            String token = shortLivedService.generateToken(userId, "testuser");

            UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
            assertThat(shortLivedService.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void extractUsername_WithMalformedToken_ShouldThrow() {
            assertThatThrownBy(() -> jwtService.extractUsername("not.a.valid.token"))
                    .isInstanceOf(Exception.class);
        }
    }
}
