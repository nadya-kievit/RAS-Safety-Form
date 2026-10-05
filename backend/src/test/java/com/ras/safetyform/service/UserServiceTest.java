package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.PasswordChangeRequest;
import com.ras.safetyform.dto.ProfileUpdateRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.dto.UserCreateRequest;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(userRepository, passwordEncoder);

    @Test
    void updateProfileChangesEditableFieldsAndPreservesRole() {
        User user = userWithPassword("old-password");
        when(user.getId()).thenReturn(7);
        when(user.getUsername()).thenReturn("new.username");
        when(user.getFirstName()).thenReturn("New");
        when(user.getLastName()).thenReturn("Name");
        when(user.getRole()).thenReturn("admin");
        when(user.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 12, 0));
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateProfile(
                7,
                new ProfileUpdateRequest(" new.username ", " New ", " Name "));

        verify(user).updateProfile("new.username", "New", "Name");
        assertEquals("admin", response.role());
    }

    @Test
    void updateProfileRejectsUsernameOwnedByAnotherUser() {
        User user = userWithPassword("old-password");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("taken", 7)).thenReturn(true);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> userService.updateProfile(
                        7,
                        new ProfileUpdateRequest("taken", "First", "Last")));

        assertEquals("That username is already in use", exception.getMessage());
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        User user = userWithPassword("old-password");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userService.changePassword(
                        7,
                        new PasswordChangeRequest(
                                "wrong-password",
                                "new-password",
                                "new-password")));

        assertEquals("Current password is incorrect", exception.getMessage());
    }

    @Test
    void changePasswordRequiresMatchingNewPasswords() {
        User user = userWithPassword("old-password");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> userService.changePassword(
                        7,
                        new PasswordChangeRequest(
                                "old-password",
                                "new-password",
                                "different-password")));

        assertEquals("New passwords do not match", exception.getMessage());
    }

    @Test
    void changePasswordStoresABcryptHash() {
        User user = userWithPassword("old-password");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        userService.changePassword(
                7,
                new PasswordChangeRequest(
                        "old-password",
                        "new-password",
                        "new-password"));

        verify(user).changePasswordHash(org.mockito.ArgumentMatchers.argThat(
                hash -> passwordEncoder.matches("new-password", hash)));
    }

    @Test
    void createUserHashesPasswordAndCreatesAnActiveUser() {
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(
                new UserCreateRequest(
                        " New ",
                        " User ",
                        " new.user ",
                        "temporary-password",
                        "framer"));

        assertEquals("new.user", response.username());
        assertEquals("framer", response.role());
        assertEquals(true, response.active());
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                passwordEncoder.matches("temporary-password", user.getPasswordHash())));
    }

    @Test
    void cannotDeactivateOwnAccount() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> userService.setUserActive(7, false, 7));

        assertEquals("You cannot deactivate your own account", exception.getMessage());
    }

    @Test
    void requireAdminRejectsInactiveAdmin() {
        User user = mock(User.class);
        when(user.getRole()).thenReturn("admin");
        when(user.isActive()).thenReturn(false);
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> userService.requireAdmin(7));

        assertEquals("Administrator access required", exception.getMessage());
    }

    private User userWithPassword(String password) {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn(passwordEncoder.encode(password));
        return user;
    }
}
