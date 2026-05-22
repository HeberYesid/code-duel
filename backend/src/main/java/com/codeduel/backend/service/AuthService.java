package com.codeduel.backend.service;

import com.codeduel.backend.dto.AuthResponse;
import com.codeduel.backend.dto.LoginRequest;
import com.codeduel.backend.dto.RegisterRequest;
import com.codeduel.backend.exception.BadRequestException;
import com.codeduel.backend.model.Profile;
import com.codeduel.backend.model.ScoreEntry;
import com.codeduel.backend.model.User;
import com.codeduel.backend.repository.ProfileRepository;
import com.codeduel.backend.repository.ScoreEntryRepository;
import com.codeduel.backend.repository.UserRepository;
import com.codeduel.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Business-level uniqueness validations (format validations are handled by DTO annotations)
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);

        // Create an empty profile for every new user
        Profile profile = Profile.builder()
                .user(user)
                .build();
        profileRepository.save(profile);

        ScoreEntry scoreEntry = ScoreEntry.builder()
                .user(user)
                .build();
        scoreEntryRepository.save(scoreEntry);

        String token = jwtService.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtService.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
