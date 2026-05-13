package com.codeduel.backend.service;

import com.codeduel.backend.dto.AuthResponse;
import com.codeduel.backend.dto.LoginRequest;
import com.codeduel.backend.dto.RegisterRequest;
import com.codeduel.backend.exception.BadRequestException;
import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.ProfileRepository;
import com.codeduel.backend.repository.UserRepository;
import com.codeduel.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest("testuser", "test@example.com", "Pass123");
        validLoginRequest = new LoginRequest("testuser", "Pass123");
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully with valid data")
        void register_WithValidData_ShouldReturnAuthResponse() {
            // Arrange
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash("hashedPassword")
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(profileRepository.save(any())).thenReturn(null);
            when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("jwt-token");

            // Act
            AuthResponse response = authService.register(validRegisterRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getUserId()).isEqualTo(savedUser.getId());

            verify(userRepository).save(any(User.class));
            verify(profileRepository).save(any());
            verify(passwordEncoder).encode("Pass123");
        }

        @Test
        @DisplayName("Should throw BadRequestException when username is already taken")
        void register_WithDuplicateUsername_ShouldThrow() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when email is already registered")
        void register_WithDuplicateEmail_ShouldThrow() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should normalize username and email to lowercase")
        void register_ShouldNormalizeUsernameAndEmail() {
            RegisterRequest request = new RegisterRequest("TestUser", "Test@Example.COM", "Pass123");
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash("hashed")
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("token");

            authService.register(request);

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should create a Profile when registering a new user")
        void register_ShouldCreateProfile() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            User savedUser = User.builder().id(UUID.randomUUID()).username("testuser").email("test@example.com").passwordHash("hashed").build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("token");

            authService.register(validRegisterRequest);

            verify(profileRepository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Should normalize username and authenticate with normalized value")
        void login_ShouldNormalizeUsernameAndAuthenticate() {
            LoginRequest request = new LoginRequest("  TeStUser  ", "Pass123");
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash("hashed")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(userId, "testuser")).thenReturn("token");

            AuthResponse response = authService.login(request);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(authCaptor.capture());
            assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("testuser");
            assertThat(authCaptor.getValue().getCredentials()).isEqualTo("Pass123");
            verify(userRepository).findByUsername("testuser");
            assertThat(response.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should throw BadRequestException when authenticated user cannot be loaded")
        void login_WhenUserMissingAfterAuthentication_ShouldThrow() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("User not found");
        }
    }
}
