package com.ras.safetyform.service;

import com.ras.safetyform.dto.LoginRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(this::invalidCredentials);

        if (!user.isActive()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }

    private AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid username or password");
    }
}
