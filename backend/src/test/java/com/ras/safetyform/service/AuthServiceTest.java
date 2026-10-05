package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.LoginRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthService authService = new AuthService(userRepository, passwordEncoder);

    @Test
    void loginReturnsUserWhenPasswordMatches() {
        User user = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        when(user.getId()).thenReturn(1);
        when(user.getFirstName()).thenReturn("Alex");
        when(user.getLastName()).thenReturn("Framer");
        when(user.getUsername()).thenReturn("alex");
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(user.getRole()).thenReturn("framer");
        when(user.getCreatedAt()).thenReturn(createdAt);
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));

        UserResponse response = authService.login(
                new LoginRequest("alex", "correct-password"));

        assertEquals(1, response.id());
        assertEquals("alex", response.username());
        assertEquals("framer", response.role());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(new LoginRequest("alex", "wrong-password")));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginRejectsUnknownUsernameWithSameMessage() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(new LoginRequest("unknown", "any-password")));

        assertEquals("Invalid username or password", exception.getMessage());
    }
}
