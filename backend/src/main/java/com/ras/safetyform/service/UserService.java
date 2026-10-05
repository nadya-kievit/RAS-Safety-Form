package com.ras.safetyform.service;

import com.ras.safetyform.dto.PasswordChangeRequest;
import com.ras.safetyform.dto.ProfileUpdateRequest;
import com.ras.safetyform.dto.UserCreateRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Integer userId) {
        return toUserResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireAdmin(Integer userId) {
        User user = findUser(userId);
        if (!user.isActive() || !"admin".equals(user.getRole())) {
            throw new AuthorizationException("Administrator access required");
        }
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new InvalidRequestException("That username is already in use");
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                username,
                passwordEncoder.encode(request.password()),
                request.role());
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setUserActive(
            Integer userId,
            boolean active,
            Integer actingUserId) {
        if (!active && userId.equals(actingUserId)) {
            throw new InvalidRequestException("You cannot deactivate your own account");
        }
        User user = findUser(userId);
        user.setActive(active);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Integer userId, ProfileUpdateRequest request) {
        User user = findUser(userId);
        String username = request.username().trim();
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();

        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new InvalidRequestException("That username is already in use");
        }

        user.updateProfile(username, firstName, lastName);
        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(Integer userId, PasswordChangeRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new InvalidRequestException("New passwords do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
