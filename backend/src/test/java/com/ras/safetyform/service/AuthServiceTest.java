package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.LoginRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.UserRepository;
import java.time.Instant;
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
        Instant createdAt = Instant.parse("2026-01-02T03:04:00Z");
        when(user.getId()).thenReturn(1);
        when(user.getFirstName()).thenReturn("Alex");
        when(user.getLastName()).thenReturn("Framer");
        when(user.getUsername()).thenReturn("alex");
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(user.getRole()).thenReturn("framer");
        when(user.isMustChangePassword()).thenReturn(true);
        when(user.isActive()).thenReturn(true);
        when(user.getCreatedAt()).thenReturn(createdAt);
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));

        UserResponse response = authService.login(
                new LoginRequest("alex", "correct-password"));

        assertEquals(1, response.id());
        assertEquals("alex", response.username());
        assertEquals("framer", response.role());
        assertEquals(true, response.mustChangePassword());
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

    @Test
    void loginExplainsDeactivationOnlyAfterTheCorrectPassword() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(user.isActive()).thenReturn(false);
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> authService.login(new LoginRequest("alex", "correct-password")));

        assertEquals("Your account has been deactivated", exception.getMessage());
    }

    @Test
    void loginDoesNotRevealDeactivationForAWrongPassword() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode("correct-password"));
        when(user.isActive()).thenReturn(false);
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.login(new LoginRequest("alex", "wrong-password")));

        assertEquals("Invalid username or password", exception.getMessage());
    }
}
