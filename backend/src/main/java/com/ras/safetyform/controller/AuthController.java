package com.ras.safetyform.controller;

import com.ras.safetyform.dto.LoginRequest;
import com.ras.safetyform.dto.PasswordChangeRequest;
import com.ras.safetyform.dto.ProfileUpdateRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.AuthService;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    static final String USER_ID_SESSION_ATTRIBUTE = "authenticatedUserId";

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        UserResponse user = authService.login(request);
        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        servletRequest.getSession(true).setAttribute(USER_ID_SESSION_ATTRIBUTE, user.id());
        return user;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(HttpServletRequest request) {
        UserResponse user = userService.getUser(requireUserId(request));
        if (!user.active()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            throw new AuthenticationException("Authentication required");
        }
        return user;
    }

    @PatchMapping("/me")
    public UserResponse updateCurrentUser(
            @Valid @RequestBody ProfileUpdateRequest profile,
            HttpServletRequest request) {
        return userService.updateProfile(requireUserId(request), profile);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody PasswordChangeRequest passwordChange,
            HttpServletRequest request) {
        userService.changePassword(requireUserId(request), passwordChange);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private Integer requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(USER_ID_SESSION_ATTRIBUTE);
        if (!(userId instanceof Integer authenticatedUserId)) {
            throw new AuthenticationException("Authentication required");
        }
        return authenticatedUserId;
    }
}
